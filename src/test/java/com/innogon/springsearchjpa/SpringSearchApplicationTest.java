package com.innogon.springsearchjpa;

import com.innogon.springsearchjpa.annotation.SearchSpec;
import com.innogon.springsearchjpa.exception.SearchQueryException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Proxy;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;
import java.util.stream.Collectors;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, classes = SpringSearchApplication.class)
@Transactional
class SpringSearchApplicationTest {

    @Autowired
    private UsersRepository userRepository;

    @Autowired
    private AuthorRepository authorRepository;

    @SuppressWarnings("unchecked")
    private static SearchSpec createSearchSpec(String searchParam, boolean caseSensitive, String[] blackList) {
        return (SearchSpec) Proxy.newProxyInstance(
                SearchSpec.class.getClassLoader(),
                new Class[]{SearchSpec.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "searchParam" -> searchParam;
                    case "caseSensitiveFlag" -> caseSensitive;
                    case "blackListedFields" -> blackList;
                    case "whiteListedFields" -> new String[]{};
                    case "required" -> false;
                    case "defaultValue" -> "";
                    case "maxLength" -> 1024;
                    case "annotationType" -> SearchSpec.class;
                    default -> method.getDefaultValue();
                }
        );
    }

    private static SearchSpec caseSensitive() {
        return createSearchSpec("", true, new String[]{});
    }

    private static SearchSpec caseInsensitive() {
        return createSearchSpec("", false, new String[]{});
    }

    private static SearchSpec withBlackList() {
        return createSearchSpec("", false, new String[]{"userFirstName"});
    }

    private static SearchSpec createAnnotation(boolean caseSensitive, String[] whiteList, String[] blackList) {
        return (SearchSpec) Proxy.newProxyInstance(
                SearchSpec.class.getClassLoader(),
                new Class[]{SearchSpec.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "searchParam" -> "";
                    case "caseSensitiveFlag" -> caseSensitive;
                    case "blackListedFields" -> blackList;
                    case "whiteListedFields" -> whiteList;
                    case "required" -> false;
                    case "defaultValue" -> "";
                    case "maxLength" -> 1024;
                    case "annotationType" -> SearchSpec.class;
                    default -> method.getDefaultValue();
                }
        );
    }

    private static SimpleDateFormat newDateFormat() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf;
    }

    @Test
    void run() {
    }

    @Test
    void canAddUsers() {
        userRepository.save(new Users());
        Assertions.assertEquals(1, userRepository.findAll((Specification<Users>) (root, query, cb) -> null).size());
    }

    @Test
    void canGetUserWithId() {
        Integer userId = userRepository.save(new Users()).getUserId();
        userRepository.save(new Users());

        Specification<Users> specification = new SpecificationsBuilder<Users>(caseSensitive())
                .withSearch("userId:" + userId).build();
        Assertions.assertEquals(userId, userRepository.findAll(specification).get(0).getUserId());
    }

    @Test
    void canGetUserWithName() {
        Integer aliceId = userRepository.save(Users.builder().userFirstName("Alice").build()).getUserId();
        userRepository.save(Users.builder().userFirstName("Bob").build());

        Specification<Users> specification = new SpecificationsBuilder<Users>(caseSensitive())
                .withSearch("userFirstName:Alice").build();
        Assertions.assertEquals(aliceId, userRepository.findAll(specification).get(0).getUserId());
    }

    @Test
    void canGetUserWithFirstNameAndLastName() {
        Integer aliceId = userRepository.save(Users.builder().userFirstName("Alice").userLastName("One").build()).getUserId();
        userRepository.save(Users.builder().userFirstName("Alice").userLastName("Two").build());
        userRepository.save(Users.builder().userFirstName("Bob").userLastName("One").build());
        userRepository.save(Users.builder().userFirstName("Bob").userLastName("Two").build());

        Specification<Users> specification = new SpecificationsBuilder<Users>(caseSensitive())
                .withSearch("userFirstName:Alice AND userLastName:One").build();
        Assertions.assertEquals(aliceId, userRepository.findAll(specification).get(0).getUserId());
    }

    @Test
    void canGetUserWithFrenchName() {
        Integer edouardProstId = userRepository.save(Users.builder().userFirstName("Édouard").userLastName("Pröst").build()).getUserId();

        Specification<Users> specification = new SpecificationsBuilder<Users>(caseSensitive())
                .withSearch("userFirstName:Édouard AND userLastName:Pröst").build();
        Assertions.assertEquals(edouardProstId, userRepository.findAll(specification).get(0).getUserId());
    }

    @Test
    void canGetUserWithChineseName() {
        Integer sunDemingId = userRepository.save(Users.builder().userFirstName("孫德明").build()).getUserId();

        Specification<Users> specification = new SpecificationsBuilder<Users>(caseSensitive())
                .withSearch("userFirstName:孫德明").build();
        Assertions.assertEquals(sunDemingId, userRepository.findAll(specification).get(0).getUserId());
    }

    @Test
    void canGetUserWithChineseNameNoEncoding() {
        Integer sunDemingId = userRepository.save(Users.builder().userFirstName("孫德明").build()).getUserId();

        Specification<Users> specification = new SpecificationsBuilder<Users>(caseSensitive())
                .withSearch("userFirstName:孫德明").build();
        Assertions.assertEquals(sunDemingId, userRepository.findAll(specification).get(0).getUserId());
    }

    @Test
    void canGetUserWithSpecialCharactersName() {
        Integer hackermanId = userRepository.save(Users.builder().userFirstName("&@#*\"''^^^$``%=+§__hack3rman__").build()).getUserId();

        Specification<Users> specification = new SpecificationsBuilder<Users>(caseSensitive())
                .withSearch("userFirstName:&@#*\"''^^^$``%=+§__hack3rman__").build();
        Assertions.assertEquals(hackermanId, userRepository.findAll(specification).get(0).getUserId());
    }

    @Test
    void canGetUserWithSpaceInNameWithString() {
        Integer robertJuniorId = userRepository.save(Users.builder().userFirstName("robert junior").build()).getUserId();

        Specification<Users> specification = new SpecificationsBuilder<Users>(caseSensitive())
                .withSearch("userFirstName:'robert junior'").build();
        Assertions.assertEquals(robertJuniorId, userRepository.findAll(specification).get(0).getUserId());
    }

    @Test
    void canGetUsersWithPartialStartingName() {
        Integer robertId = userRepository.save(Users.builder().userFirstName("robert").build()).getUserId();
        Integer robertaId = userRepository.save(Users.builder().userFirstName("roberta").build()).getUserId();
        userRepository.save(Users.builder().userFirstName("robot").build());
        userRepository.save(Users.builder().userFirstName("röbert").build());

        Specification<Users> specification = new SpecificationsBuilder<Users>(caseSensitive())
                .withSearch("userFirstName:robe*").build();
        List<Users> robeUsers = userRepository.findAll(specification);
        Assertions.assertTrue(Set.of(robertId, robertaId).equals(robeUsers.stream().map(Users::getUserId).collect(Collectors.toSet())));
    }

    @Test
    void canGetUsersWithPartialEndingName() {
        Integer robertId = userRepository.save(Users.builder().userFirstName("robert").build()).getUserId();
        Integer roubertId = userRepository.save(Users.builder().userFirstName("roubert").build()).getUserId();
        userRepository.save(Users.builder().userFirstName("robot").build());
        userRepository.save(Users.builder().userFirstName("röbęrt").build());

        Specification<Users> specification = new SpecificationsBuilder<Users>(caseSensitive())
                .withSearch("userFirstName:*ert").build();
        List<Users> robeUsers = userRepository.findAll(specification);
        Assertions.assertTrue(Set.of(robertId, roubertId).equals(robeUsers.stream().map(Users::getUserId).collect(Collectors.toSet())));
    }

    @Test
    void canGetUsersWithPartialNameAndSpecialCharacter() {
        Integer robertId = userRepository.save(Users.builder().userFirstName("rob*rt").build()).getUserId();
        Integer robertaId = userRepository.save(Users.builder().userFirstName("rob*rta").build()).getUserId();
        userRepository.save(Users.builder().userFirstName("robot").build());
        userRepository.save(Users.builder().userFirstName("röb*rt").build());

        Specification<Users> specification = new SpecificationsBuilder<Users>(caseSensitive())
                .withSearch("userFirstName:rob**").build();
        List<Users> robeUsers = userRepository.findAll(specification);
        Assertions.assertTrue(Set.of(robertId, robertaId).equals(robeUsers.stream().map(Users::getUserId).collect(Collectors.toSet())));
    }

    @Test
    void canGetUsersWithPartialNameContaining() {
        Integer robertId = userRepository.save(Users.builder().userFirstName("Robert").build()).getUserId();
        Integer robertaId = userRepository.save(Users.builder().userFirstName("Roberta").build()).getUserId();
        Integer toborobeId = userRepository.save(Users.builder().userFirstName("Toborobe").build()).getUserId();
        Integer obertaId = userRepository.save(Users.builder().userFirstName("oberta").build()).getUserId();
        userRepository.save(Users.builder().userFirstName("Robot").build());
        userRepository.save(Users.builder().userFirstName("Röbert").build());

        Specification<Users> specification = new SpecificationsBuilder<Users>(caseSensitive())
                .withSearch("userFirstName:*obe*").build();
        List<Users> specificationUsers = userRepository.findAll(specification);
        Assertions.assertTrue(
                Set.of(robertId, robertaId, toborobeId, obertaId).equals(
                        specificationUsers.stream().map(Users::getUserId).collect(Collectors.toSet())));
    }

    @Test
    void canGetUsersWithPartialNameContainingWithSpecialCharacter() {
        Integer robertId = userRepository.save(Users.builder().userFirstName("Rob*rt").build()).getUserId();
        Integer robertaId = userRepository.save(Users.builder().userFirstName("rob*rta").build()).getUserId();
        Integer lobertaId = userRepository.save(Users.builder().userFirstName("Lob*rta").build()).getUserId();
        Integer tobertaId = userRepository.save(Users.builder().userFirstName("Tob*rta").build()).getUserId();
        userRepository.save(Users.builder().userFirstName("robot").build());
        userRepository.save(Users.builder().userFirstName("röb*rt").build());

        Specification<Users> specification = new SpecificationsBuilder<Users>(caseSensitive())
                .withSearch("userFirstName:*ob**").build();
        List<Users> specificationUsers = userRepository.findAll(specification);
        Assertions.assertTrue(
                Set.of(robertId, robertaId, lobertaId, tobertaId).equals(
                        specificationUsers.stream().map(Users::getUserId).collect(Collectors.toSet())));
    }

    @Test
    void canGetUsersWithPartialNameContainingSpecialCharacterUsingSimpleString() {
        Integer robertId = userRepository.save(Users.builder().userFirstName("Rob*rt").build()).getUserId();
        Integer robertaId = userRepository.save(Users.builder().userFirstName("rob*rta").build()).getUserId();
        Integer lobertaId = userRepository.save(Users.builder().userFirstName("Lob*rta").build()).getUserId();
        Integer tobertaId = userRepository.save(Users.builder().userFirstName("Tob*rta").build()).getUserId();
        userRepository.save(Users.builder().userFirstName("robot").build());
        userRepository.save(Users.builder().userFirstName("röb*rt").build());

        Specification<Users> specification = new SpecificationsBuilder<Users>(caseSensitive())
                .withSearch("userFirstName:'*ob**'").build();
        List<Users> specificationUsers = userRepository.findAll(specification);
        Assertions.assertTrue(
                Set.of(robertId, robertaId, lobertaId, tobertaId).equals(
                        specificationUsers.stream().map(Users::getUserId).collect(Collectors.toSet())));
    }

    @Test
    void canGetUsersWithPartialNameContainingWithSpecialCharacterUsingDoubleString() {
        Integer robertId = userRepository.save(Users.builder().userFirstName("Rob*rt").build()).getUserId();
        Integer robertaId = userRepository.save(Users.builder().userFirstName("rob*rta").build()).getUserId();
        Integer lobertaId = userRepository.save(Users.builder().userFirstName("Lob*rta").build()).getUserId();
        Integer tobertaId = userRepository.save(Users.builder().userFirstName("Tob*rta").build()).getUserId();
        userRepository.save(Users.builder().userFirstName("robot").build());
        userRepository.save(Users.builder().userFirstName("röb*rt").build());

        Specification<Users> specification = new SpecificationsBuilder<Users>(caseSensitive())
                .withSearch("userFirstName:\"*ob**\"").build();
        List<Users> specificationUsers = userRepository.findAll(specification);
        Assertions.assertTrue(
                Set.of(robertId, robertaId, lobertaId, tobertaId).equals(
                        specificationUsers.stream().map(Users::getUserId).collect(Collectors.toSet())));
    }

    @Test
    void canGetUsersNotContaining() {
        Integer lobertaId = userRepository.save(Users.builder().userFirstName("Lobérta").build()).getUserId();
        Integer tobertaId = userRepository.save(Users.builder().userFirstName("Toberta").build()).getUserId();
        Integer robotId = userRepository.save(Users.builder().userFirstName("robot").build()).getUserId();
        Integer roobertId = userRepository.save(Users.builder().userFirstName("röbert").build()).getUserId();
        userRepository.save(Users.builder().userFirstName("Robèrt").build());
        userRepository.save(Users.builder().userFirstName("robèrta").build());

        Specification<Users> specification = new SpecificationsBuilder<Users>(caseSensitive())
                .withSearch("userFirstName!*è*").build();
        List<Users> specificationUsers = userRepository.findAll(specification);
        Assertions.assertTrue(
                Set.of(lobertaId, tobertaId, robotId, roobertId).equals(
                        specificationUsers.stream().map(Users::getUserId).collect(Collectors.toSet())));
    }

    @Test
    void canGetUsersNotStartingWith() {
        Integer aliceId = userRepository.save(Users.builder().userFirstName("Alice").build()).getUserId();
        Integer aliceId2 = userRepository.save(Users.builder().userFirstName("alice").build()).getUserId();
        Integer bobId = userRepository.save(Users.builder().userFirstName("bob").build()).getUserId();
        userRepository.save(Users.builder().userFirstName("Bob").build());

        Specification<Users> specification = new SpecificationsBuilder<Users>(caseSensitive())
                .withSearch("userFirstName!B*").build();
        List<Users> specificationUsers = userRepository.findAll(specification);
        Assertions.assertTrue(Set.of(aliceId, aliceId2, bobId).equals(specificationUsers.stream().map(Users::getUserId).collect(Collectors.toSet())));
    }

    @Test
    void canGetUsersNotEndingWith() {
        Integer bobId = userRepository.save(Users.builder().userFirstName("bob").build()).getUserId();
        Integer alicEId = userRepository.save(Users.builder().userFirstName("alicE").build()).getUserId();
        Integer boBId = userRepository.save(Users.builder().userFirstName("boB").build()).getUserId();
        userRepository.save(Users.builder().userFirstName("alice").build());

        Specification<Users> specification = new SpecificationsBuilder<Users>(caseSensitive())
                .withSearch("userFirstName!*e").build();
        List<Users> specificationUsers = userRepository.findAll(specification);
        Assertions.assertTrue(Set.of(boBId, alicEId, bobId).equals(specificationUsers.stream().map(Users::getUserId).collect(Collectors.toSet())));
    }

    @Test
    void canGetUserWithBigFamily() {
        Integer userWith5ChildrenId = userRepository.save(Users.builder().userChildrenNumber(5).build()).getUserId();
        Integer userWith6ChildrenId = userRepository.save(Users.builder().userChildrenNumber(6).build()).getUserId();
        Integer user2With5ChildrenId = userRepository.save(Users.builder().userChildrenNumber(5).build()).getUserId();
        userRepository.save(Users.builder().userChildrenNumber(1).build());
        userRepository.save(Users.builder().userChildrenNumber(2).build());
        userRepository.save(Users.builder().userChildrenNumber(4).build());
        userRepository.save(Users.builder().userChildrenNumber(2).build());

        Specification<Users> specification = new SpecificationsBuilder<Users>(caseSensitive())
                .withSearch("userChildrenNumber>4").build();
        List<Users> specificationUsers = userRepository.findAll(specification);
        Assertions.assertTrue(
                Set.of(user2With5ChildrenId, userWith5ChildrenId, userWith6ChildrenId).equals(
                        specificationUsers.stream().map(Users::getUserId).collect(Collectors.toSet())));
    }

    @Test
    void canGetUserWithSmallFamily() {
        userRepository.save(Users.builder().userChildrenNumber(5).build());
        userRepository.save(Users.builder().userChildrenNumber(6).build());
        userRepository.save(Users.builder().userChildrenNumber(5).build());
        Integer userWith1ChildrenId = userRepository.save(Users.builder().userChildrenNumber(1).build()).getUserId();
        Integer userWith2ChildrenId = userRepository.save(Users.builder().userChildrenNumber(2).build()).getUserId();
        userRepository.save(Users.builder().userChildrenNumber(4).build());
        Integer user2With2ChildrenId = userRepository.save(Users.builder().userChildrenNumber(2).build()).getUserId();

        Specification<Users> specification = new SpecificationsBuilder<Users>(caseSensitive())
                .withSearch("userChildrenNumber<4").build();
        List<Users> specificationUsers = userRepository.findAll(specification);
        Assertions.assertTrue(
                Set.of(user2With2ChildrenId, userWith1ChildrenId, userWith2ChildrenId).equals(
                        specificationUsers.stream().map(Users::getUserId).collect(Collectors.toSet())));
    }

    @Test
    void canGetUserWithChildrenEquals() {
        Integer user1With4ChildrenId = userRepository.save(Users.builder().userChildrenNumber(4).build()).getUserId();
        Integer user2With4ChildrenId = userRepository.save(Users.builder().userChildrenNumber(4).build()).getUserId();
        Integer user3With4ChildrenId = userRepository.save(Users.builder().userChildrenNumber(4).build()).getUserId();
        userRepository.save(Users.builder().userChildrenNumber(5).build());
        userRepository.save(Users.builder().userChildrenNumber(1).build());
        userRepository.save(Users.builder().userChildrenNumber(2).build());
        userRepository.save(Users.builder().userChildrenNumber(6).build());
        userRepository.save(Users.builder().userChildrenNumber(2).build());
        userRepository.save(Users.builder().userChildrenNumber(5).build());

        Specification<Users> specification = new SpecificationsBuilder<Users>(caseSensitive())
                .withSearch("userChildrenNumber:4").build();
        List<Users> specificationUsers = userRepository.findAll(specification);
        Assertions.assertTrue(
                Set.of(user1With4ChildrenId, user2With4ChildrenId, user3With4ChildrenId).equals(
                        specificationUsers.stream().map(Users::getUserId).collect(Collectors.toSet())));
    }

    @Test
    void canGetUserWithChildrenNotEquals() {
        Integer userWith5ChildrenId = userRepository.save(Users.builder().userChildrenNumber(5).build()).getUserId();
        Integer userWith1ChildId = userRepository.save(Users.builder().userChildrenNumber(1).build()).getUserId();
        Integer userWith6ChildrenId = userRepository.save(Users.builder().userChildrenNumber(6).build()).getUserId();
        userRepository.save(Users.builder().userChildrenNumber(2).build());
        userRepository.save(Users.builder().userChildrenNumber(2).build());

        Specification<Users> specification = new SpecificationsBuilder<Users>(caseSensitive())
                .withSearch("userChildrenNumber!2").build();
        List<Users> specificationUsers = userRepository.findAll(specification);
        Assertions.assertTrue(
                Set.of(userWith1ChildId, userWith5ChildrenId, userWith6ChildrenId).equals(
                        specificationUsers.stream().map(Users::getUserId).collect(Collectors.toSet())));
    }

    @Test
    void canGetUserWithSmallerSalary() {
        Integer smallerSalaryUserId = userRepository.save(Users.builder().userSalary(2223.3F).build()).getUserId();
        Integer smallerSalaryUser2Id = userRepository.save(Users.builder().userSalary(1500.2F).build()).getUserId();
        userRepository.save(Users.builder().userSalary(4000.0F).build());
        userRepository.save(Users.builder().userSalary(2550.7F).build());
        userRepository.save(Users.builder().userSalary(2300.0F).build());

        Specification<Users> specification = new SpecificationsBuilder<Users>(caseSensitive())
                .withSearch("userSalary<2300").build();
        List<Users> specificationUsers = userRepository.findAll(specification);
        Assertions.assertTrue(
                Set.of(smallerSalaryUserId, smallerSalaryUser2Id).equals(
                        specificationUsers.stream().map(Users::getUserId).collect(Collectors.toSet())));
    }

    @Test
    void canGetUserWithHigherFloatSalary() {
        Integer higherSalaryUserId = userRepository.save(Users.builder().userSalary(4000.1F).build()).getUserId();
        Integer higherSalaryUser2Id = userRepository.save(Users.builder().userSalary(5350.7F).build()).getUserId();
        userRepository.save(Users.builder().userSalary(2323.3F).build());
        userRepository.save(Users.builder().userSalary(1500.2F).build());

        Specification<Users> specification = new SpecificationsBuilder<Users>(caseSensitive())
                .withSearch("userSalary>4000.001").build();
        List<Users> specificationUsers = userRepository.findAll(specification);
        Assertions.assertTrue(
                Set.of(higherSalaryUserId, higherSalaryUser2Id).equals(
                        specificationUsers.stream().map(Users::getUserId).collect(Collectors.toSet())));
    }

    @Test
    void canGetUserWithMedianSalary() {
        Integer medianUserId = userRepository.save(Users.builder().userSalary(2323.3F).build()).getUserId();
        userRepository.save(Users.builder().userSalary(1500.2F).build());
        userRepository.save(Users.builder().userSalary(4000.1F).build());
        userRepository.save(Users.builder().userSalary(5350.7F).build());

        Specification<Users> specification = new SpecificationsBuilder<Users>(caseSensitive())
                .withSearch("userSalary<4000.1 AND userSalary>1500.2").build();
        List<Users> specificationUsers = userRepository.findAll(specification);
        Assertions.assertTrue(Set.of(medianUserId).equals(specificationUsers.stream().map(Users::getUserId).collect(Collectors.toSet())));
    }

    @Test
    void canGetUsersWithAgeHigher() {
        Integer olderUserId = userRepository.save(Users.builder().userAgeInSeconds(23222223.3).build()).getUserId();
        userRepository.save(Users.builder().userAgeInSeconds(23222223.2).build());
        userRepository.save(Users.builder().userAgeInSeconds(23222223.0).build());

        Specification<Users> specification = new SpecificationsBuilder<Users>(caseSensitive())
                .withSearch("userAgeInSeconds>23222223.2").build();
        List<Users> specificationUsers = userRepository.findAll(specification);
        Assertions.assertTrue(Set.of(olderUserId).equals(specificationUsers.stream().map(Users::getUserId).collect(Collectors.toSet())));
    }

    @Test
    void canGetUsersWithAgeLower() {
        Integer youngerUserId = userRepository.save(Users.builder().userAgeInSeconds(23222223.0).build()).getUserId();
        userRepository.save(Users.builder().userAgeInSeconds(23222223.2).build());
        userRepository.save(Users.builder().userAgeInSeconds(23222223.3).build());

        Specification<Users> specification = new SpecificationsBuilder<Users>(caseSensitive())
                .withSearch("userAgeInSeconds<23222223.2").build();
        List<Users> specificationUsers = userRepository.findAll(specification);
        Assertions.assertTrue(Set.of(youngerUserId).equals(specificationUsers.stream().map(Users::getUserId).collect(Collectors.toSet())));
    }

    @Test
    void canGetUsersWithAgeEqual() {
        Integer middleUserId = userRepository.save(Users.builder().userAgeInSeconds(23222223.2).build()).getUserId();
        userRepository.save(Users.builder().userAgeInSeconds(23222223.3).build());
        userRepository.save(Users.builder().userAgeInSeconds(23222223.0).build());

        Specification<Users> specification = new SpecificationsBuilder<Users>(caseSensitive())
                .withSearch("userAgeInSeconds:23222223.2").build();
        List<Users> specificationUsers = userRepository.findAll(specification);
        Assertions.assertTrue(Set.of(middleUserId).equals(specificationUsers.stream().map(Users::getUserId).collect(Collectors.toSet())));
    }

    @Test
    void canGetUserWithParentheses() {
        Integer userOneId = userRepository.save(Users.builder().userSalary(1500.2F).userLastName("One").build()).getUserId();
        Integer userTwoId = userRepository.save(Users.builder().userSalary(1500.2F).userLastName("Two").build()).getUserId();
        userRepository.save(Users.builder().userSalary(1500.1F).userLastName("One").build());
        userRepository.save(Users.builder().userSalary(1500.1F).userLastName("Two").build());
        userRepository.save(Users.builder().userSalary(1500.1F).userLastName("Three").build());
        userRepository.save(Users.builder().userSalary(1500.2F).userLastName("Three").build());

        Specification<Users> specification = new SpecificationsBuilder<Users>(caseSensitive())
                .withSearch("userSalary>1500.1 AND ( userLastName:One OR userLastName:Two )").build();
        List<Users> specificationUsers = userRepository.findAll(specification);
        Assertions.assertTrue(
                Set.of(userOneId, userTwoId).equals(
                        specificationUsers.stream().map(Users::getUserId).collect(Collectors.toSet())));
    }

    @Test
    void canGetUsersWithInterlinkedConditions() {
        Integer userOneId = userRepository.save(Users.builder().userSalary(1501F).userLastName("One").build()).getUserId();
        Integer userOeId = userRepository.save(Users.builder().userSalary(1501F).userLastName("Oe").build()).getUserId();
        userRepository.save(Users.builder().userSalary(1501F).userLastName("One one").build());
        userRepository.save(Users.builder().userSalary(1501F).userLastName("Oneone").build());
        userRepository.save(Users.builder().userSalary(1501F).userLastName("O n e").build());
        userRepository.save(Users.builder().userSalary(1502F).userLastName("One").build());

        Specification<Users> specification = new SpecificationsBuilder<Users>(caseSensitive())
                .withSearch("userSalary<1502 AND ( ( userLastName:One OR userLastName:one ) OR userLastName!*n* )").build();
        List<Users> specificationUsers = userRepository.findAll(specification);
        Assertions.assertTrue(
                Set.of(userOneId, userOeId).equals(
                        specificationUsers.stream().map(Users::getUserId).collect(Collectors.toSet())));
    }

    @Test
    void canGetUsersWithInterlinkedConditionsNoSpaces() {
        Integer userOneId = userRepository.save(Users.builder().userSalary(1501F).userLastName("One").build()).getUserId();
        Integer userOeId = userRepository.save(Users.builder().userSalary(1501F).userLastName("Oe").build()).getUserId();
        userRepository.save(Users.builder().userSalary(1501F).userLastName("One one").build());
        userRepository.save(Users.builder().userSalary(1501F).userLastName("Oneone").build());
        userRepository.save(Users.builder().userSalary(1501F).userLastName("O n e").build());
        userRepository.save(Users.builder().userSalary(1502F).userLastName("One").build());

        Specification<Users> specification = new SpecificationsBuilder<Users>(caseSensitive())
                .withSearch("userSalary<1502 AND ((userLastName:One OR userLastName:one) OR userLastName!*n*)").build();
        List<Users> specificationUsers = userRepository.findAll(specification);
        Assertions.assertTrue(
                Set.of(userOneId, userOeId).equals(
                        specificationUsers.stream().map(Users::getUserId).collect(Collectors.toSet())));
    }

    @Test
    void canGetUsersByBoolean() {
        userRepository.save(Users.builder().isAdmin(true).build());
        userRepository.save(Users.builder().isAdmin(false).build());

        Specification<Users> specification = new SpecificationsBuilder<Users>(caseSensitive())
                .withSearch("isAdmin:true").build();
        List<Users> specificationUsers = userRepository.findAll(specification);
        Assertions.assertEquals(1, specificationUsers.size());
    }

    @Test
    void canGetUsersEarlierThanDate() throws ParseException {
        SimpleDateFormat sdf = newDateFormat();
        userRepository.save(Users.builder().createdAt(sdf.parse("2019-01-01")).build());
        userRepository.save(Users.builder().createdAt(sdf.parse("2019-01-03")).build());

        Specification<Users> specification = new SpecificationsBuilder<Users>(caseSensitive())
                .withSearch("createdAt<'2019-01-02'").build();
        List<Users> specificationUsers = userRepository.findAll(specification);
        Assertions.assertEquals(1, specificationUsers.size());
    }

    @Test
    void canGetUsersAfterDate() throws ParseException {
        SimpleDateFormat sdf = newDateFormat();
        userRepository.save(Users.builder().createdAt(sdf.parse("2019-01-01")).build());
        userRepository.save(Users.builder().createdAt(sdf.parse("2019-01-03")).build());

        Specification<Users> specification = new SpecificationsBuilder<Users>(caseSensitive())
                .withSearch("createdAt>'2019-01-02'").build();
        List<Users> specificationUsers = userRepository.findAll(specification);
        Assertions.assertEquals(1, specificationUsers.size());

        specification = new SpecificationsBuilder<Users>(caseSensitive())
                .withSearch("createdAt>'2019-01-04'").build();
        specificationUsers = userRepository.findAll(specification);
        Assertions.assertEquals(0, specificationUsers.size());
    }

    @Test
    void canGetUsersAfterEqualDate() throws ParseException {
        SimpleDateFormat sdf = newDateFormat();
        userRepository.save(Users.builder().createdAt(sdf.parse("2019-01-01")).build());
        userRepository.save(Users.builder().createdAt(sdf.parse("2019-01-03")).build());

        Specification<Users> specification = new SpecificationsBuilder<Users>(caseSensitive())
                .withSearch("createdAt>:'2019-01-01'").build();
        List<Users> specificationUsers = userRepository.findAll(specification);
        Assertions.assertEquals(2, specificationUsers.size());

        specification = new SpecificationsBuilder<Users>(caseSensitive())
                .withSearch("createdAt>:'2019-01-04'").build();
        specificationUsers = userRepository.findAll(specification);
        Assertions.assertEquals(0, specificationUsers.size());
    }

    @Test
    void canGetUsersEarlierEqualDate() throws ParseException {
        SimpleDateFormat sdf = newDateFormat();
        userRepository.save(Users.builder().createdAt(sdf.parse("2019-01-01")).build());
        userRepository.save(Users.builder().createdAt(sdf.parse("2019-01-03")).build());

        Specification<Users> specification = new SpecificationsBuilder<Users>(caseSensitive())
                .withSearch("createdAt<:'2019-01-01'").build();
        List<Users> specificationUsers = userRepository.findAll(specification);
        Assertions.assertEquals(1, specificationUsers.size());

        specification = new SpecificationsBuilder<Users>(caseSensitive())
                .withSearch("createdAt<:'2019-01-03'").build();
        specificationUsers = userRepository.findAll(specification);
        Assertions.assertEquals(2, specificationUsers.size());
    }

    @Test
    void canGetUsersAtPreciseDate() throws ParseException {
        SimpleDateFormat sdf = newDateFormat();
        Date date = sdf.parse("2019-01-01");
        userRepository.save(Users.builder().createdAt(date).build());

        Specification<Users> specification = new SpecificationsBuilder<Users>(caseSensitive())
                .withSearch("createdAt:'" + sdf.format(date) + "'").build();
        List<Users> specificationUsers = userRepository.findAll(specification);
        Assertions.assertEquals(1, specificationUsers.size());
    }

    @Test
    void canGetUsersAtPreciseDateNotEqual() throws ParseException {
        SimpleDateFormat sdf = newDateFormat();
        Date date = sdf.parse("2019-01-01");
        userRepository.save(Users.builder().createdAt(date).build());

        Specification<Users> specification = new SpecificationsBuilder<Users>(caseSensitive())
                .withSearch("createdAt!'2019-01-02'").build();
        List<Users> specificationUsers = userRepository.findAll(specification);
        Assertions.assertEquals(1, specificationUsers.size());
    }

    @Test
    void canGetUsersWithCaseInsensitiveLowerCaseSearch() {
        Integer robertId = userRepository.save(Users.builder().userFirstName("ROBERT").build()).getUserId();
        Integer robertaId = userRepository.save(Users.builder().userFirstName("roberta").build()).getUserId();
        userRepository.save(Users.builder().userFirstName("robot").build());
        userRepository.save(Users.builder().userFirstName("röbęrt").build());

        Specification<Users> specification = new SpecificationsBuilder<Users>(caseInsensitive())
                .withSearch("userFirstName:robe*").build();
        List<Users> robeUsers = userRepository.findAll(specification);
        Assertions.assertTrue(Set.of(robertId, robertaId).equals(robeUsers.stream().map(Users::getUserId).collect(Collectors.toSet())));
    }

    @Test
    void canGetUsersWithCaseInsensitiveUpperCaseSearch() {
        Integer robertId = userRepository.save(Users.builder().userFirstName("ROBERT").build()).getUserId();
        Integer roubertId = userRepository.save(Users.builder().userFirstName("roubert").build()).getUserId();
        userRepository.save(Users.builder().userFirstName("robot").build());
        userRepository.save(Users.builder().userFirstName("röbęrt").build());

        Specification<Users> specification = new SpecificationsBuilder<Users>(caseInsensitive())
                .withSearch("userFirstName:*ert").build();
        List<Users> robeUsers = userRepository.findAll(specification);
        Assertions.assertTrue(Set.of(robertId, roubertId).equals(robeUsers.stream().map(Users::getUserId).collect(Collectors.toSet())));
    }

    @Test
    void canGetUsersWithCaseInsensitiveContainsSearch() {
        Integer robertId = userRepository.save(Users.builder().userFirstName("ROBERT").build()).getUserId();
        Integer roubertId = userRepository.save(Users.builder().userFirstName("roubert").build()).getUserId();
        userRepository.save(Users.builder().userFirstName("robot").build());
        userRepository.save(Users.builder().userFirstName("röbęrt").build());

        Specification<Users> specification = new SpecificationsBuilder<Users>(caseInsensitive())
                .withSearch("userFirstName:*er*").build();
        List<Users> robeUsers = userRepository.findAll(specification);
        Assertions.assertTrue(Set.of(robertId, roubertId).equals(robeUsers.stream().map(Users::getUserId).collect(Collectors.toSet())));
    }

    @Test
    void canGetUsersWithCaseInsensitiveDoesntContainSearch() {
        userRepository.save(Users.builder().userFirstName("ROBERT").build());
        Integer roubertId = userRepository.save(Users.builder().userFirstName("roubert").build()).getUserId();
        userRepository.save(Users.builder().userFirstName("robot").build());

        Specification<Users> specification = new SpecificationsBuilder<Users>(caseInsensitive())
                .withSearch("userFirstName!*rob*").build();
        List<Users> robeUsers = userRepository.findAll(specification);
        Assertions.assertTrue(Set.of(roubertId).equals(robeUsers.stream().map(Users::getUserId).collect(Collectors.toSet())));
    }

    @Test
    void canGetUsersWithCaseInsensitiveDoesntStartSearch() {
        userRepository.save(Users.builder().userFirstName("ROBERT").build());
        Integer roubertId = userRepository.save(Users.builder().userFirstName("roubert").build()).getUserId();
        userRepository.save(Users.builder().userFirstName("robot").build());

        Specification<Users> specification = new SpecificationsBuilder<Users>(caseInsensitive())
                .withSearch("userFirstName!rob*").build();
        List<Users> robeUsers = userRepository.findAll(specification);
        Assertions.assertTrue(Set.of(roubertId).equals(robeUsers.stream().map(Users::getUserId).collect(Collectors.toSet())));
    }

    @Test
    void canGetUsersWithCaseInsensitiveDoesntEndSearch() {
        userRepository.save(Users.builder().userFirstName("ROBERT").build());
        userRepository.save(Users.builder().userFirstName("roubert").build());
        Integer robotId = userRepository.save(Users.builder().userFirstName("robot").build()).getUserId();

        Specification<Users> specification = new SpecificationsBuilder<Users>(caseInsensitive())
                .withSearch("userFirstName!*rt").build();
        List<Users> robotUsers = userRepository.findAll(specification);
        Assertions.assertTrue(Set.of(robotId).equals(robotUsers.stream().map(Users::getUserId).collect(Collectors.toSet())));
    }

    @Test
    void canGetUsersWithUserTypeEqualSearch() {
        userRepository.save(Users.builder().userFirstName("Hamid").type(UserType.TEAM_MEMBER).build());
        userRepository.save(Users.builder().userFirstName("Reza").type(UserType.TEAM_MEMBER).build());
        userRepository.save(Users.builder().userFirstName("Ireh").type(UserType.TEAM_MEMBER).build());
        userRepository.save(Users.builder().userFirstName("robot").type(UserType.ADMINISTRATOR).build());

        Specification<Users> specification = new SpecificationsBuilder<Users>(caseSensitive())
                .withSearch("type:ADMINISTRATOR").build();
        List<Users> robeUsers = userRepository.findAll(specification);
        Assertions.assertEquals(1, robeUsers.size());
        Assertions.assertEquals("robot", robeUsers.get(0).getUserFirstName());
    }

    @Test
    void canGetUsersWithUserTypeNotEqualSearch() {
        userRepository.save(Users.builder().userFirstName("HamidReza").type(UserType.TEAM_MEMBER).build());
        userRepository.save(Users.builder().userFirstName("Ireh").type(UserType.ADMINISTRATOR).build());
        userRepository.save(Users.builder().userFirstName("robot").type(UserType.TEAM_MEMBER).build());

        Specification<Users> specification = new SpecificationsBuilder<Users>(caseSensitive())
                .withSearch("type!TEAM_MEMBER").build();
        List<Users> irehUsers = userRepository.findAll(specification);
        Assertions.assertEquals(1, irehUsers.size());
        Assertions.assertEquals("Ireh", irehUsers.get(0).getUserFirstName());
    }

    @Test
    void canGetUsersWithUpdatedAtGreaterSearch() {
        userRepository.save(Users.builder().userFirstName("HamidReza").updatedAt(LocalDateTime.parse("2020-01-10T10:15:30")).build());
        userRepository.save(Users.builder().userFirstName("robot").updatedAt(LocalDateTime.parse("2020-01-11T10:20:30")).build());

        Specification<Users> specification = new SpecificationsBuilder<Users>(caseSensitive())
                .withSearch("updatedAt>'2020-01-11T09:20:30'").build();
        List<Users> robotUsers = userRepository.findAll(specification);
        Assertions.assertEquals(1, robotUsers.size());
        Assertions.assertEquals("robot", robotUsers.get(0).getUserFirstName());
    }

    @Test
    void canGetUsersWithUpdateInstantAtGreaterSearch() {
        userRepository.save(Users.builder().userFirstName("HamidReza").updatedInstantAt(Instant.parse("2020-01-10T10:15:30Z")).build());
        userRepository.save(Users.builder().userFirstName("robot").updatedInstantAt(Instant.parse("2020-01-11T10:20:30Z")).build());

        Specification<Users> specification = new SpecificationsBuilder<Users>(caseSensitive())
                .withSearch("updatedInstantAt>'2020-01-11T09:20:30Z'").build();
        List<Users> robotUsers = userRepository.findAll(specification);
        Assertions.assertEquals(1, robotUsers.size());
        Assertions.assertEquals("robot", robotUsers.get(0).getUserFirstName());
    }

    @Test
    void canGetUsersWithUpdateInstantAtGreaterThanEqualSearch() {
        userRepository.save(Users.builder().userFirstName("john").updatedInstantAt(Instant.parse("2020-01-10T10:15:30Z")).build());
        userRepository.save(Users.builder().userFirstName("robot").updatedInstantAt(Instant.parse("2020-01-11T10:20:30Z")).build());

        Specification<Users> specification = new SpecificationsBuilder<Users>(caseSensitive())
                .withSearch("updatedInstantAt>:'2020-01-11T09:20:30Z'").build();
        List<Users> robotUsers = userRepository.findAll(specification);
        Assertions.assertEquals(1, robotUsers.size());
        Assertions.assertEquals("robot", robotUsers.get(0).getUserFirstName());
    }

    @Test
    void canGetUsersWithUpdateInstantAtLessThanEqualSearch() {
        userRepository.save(Users.builder().userFirstName("john").updatedInstantAt(Instant.parse("2020-01-10T10:15:30Z")).build());
        userRepository.save(Users.builder().userFirstName("robot").updatedInstantAt(Instant.parse("2020-01-11T10:20:30Z")).build());

        Specification<Users> specification = new SpecificationsBuilder<Users>(caseSensitive())
                .withSearch("updatedInstantAt<:'2020-01-11T09:20:30Z'").build();
        List<Users> robotUsers = userRepository.findAll(specification);
        Assertions.assertEquals(1, robotUsers.size());
        Assertions.assertEquals("john", robotUsers.get(0).getUserFirstName());
    }

    @Test
    void canGetUsersWithUpdatedAtLessSearch() {
        userRepository.save(Users.builder().userFirstName("HamidReza").updatedAt(LocalDateTime.parse("2020-01-10T10:15:30")).build());
        userRepository.save(Users.builder().userFirstName("robot").updatedAt(LocalDateTime.parse("2020-01-11T10:20:30")).build());

        Specification<Users> specification = new SpecificationsBuilder<Users>(caseSensitive())
                .withSearch("updatedAt<'2020-01-11T09:20:30'").build();
        List<Users> hamidrezaUsers = userRepository.findAll(specification);
        Assertions.assertEquals(1, hamidrezaUsers.size());
        Assertions.assertEquals("HamidReza", hamidrezaUsers.get(0).getUserFirstName());
    }

    @Test
    void canGetUsersWithUpdatedAtEqualSearch() {
        userRepository.save(Users.builder().userFirstName("HamidReza").updatedAt(LocalDateTime.parse("2020-01-10T10:15:30")).build());
        userRepository.save(Users.builder().userFirstName("robot").updatedAt(LocalDateTime.parse("2020-01-11T10:20:30")).build());

        Specification<Users> specification = new SpecificationsBuilder<Users>(caseSensitive())
                .withSearch("updatedAt:'2020-01-10T10:15:30'").build();
        List<Users> hamidrezaUsers = userRepository.findAll(specification);
        Assertions.assertEquals(1, hamidrezaUsers.size());
        Assertions.assertEquals("HamidReza", hamidrezaUsers.get(0).getUserFirstName());
    }

    @Test
    void canGetUsersWithUpdatedAtNotEqualSearch() {
        userRepository.save(Users.builder().userFirstName("HamidReza").updatedAt(LocalDateTime.parse("2020-01-10T10:15:30")).build());
        userRepository.save(Users.builder().userFirstName("robot").updatedAt(LocalDateTime.parse("2020-01-11T10:20:30")).build());

        Specification<Users> specification = new SpecificationsBuilder<Users>(caseSensitive())
                .withSearch("updatedAt!'2020-01-11T10:20:30'").build();
        List<Users> hamidrezaUsers = userRepository.findAll(specification);
        Assertions.assertEquals(1, hamidrezaUsers.size());
        Assertions.assertEquals("HamidReza", hamidrezaUsers.get(0).getUserFirstName());
    }

    @Test
    void canGetUsersWithUpdatedAtGreaterThanEqualSearch() {
        userRepository.save(Users.builder().userFirstName("john").updatedAt(LocalDateTime.parse("2020-01-10T10:15:30")).build());
        userRepository.save(Users.builder().userFirstName("robot").updatedAt(LocalDateTime.parse("2020-01-11T10:20:30")).build());
        userRepository.save(Users.builder().userFirstName("robot2").updatedAt(LocalDateTime.parse("2020-01-12T10:20:30")).build());
        Specification<Users> specification = new SpecificationsBuilder<Users>(caseSensitive())
                .withSearch("updatedAt>:'2020-01-11T10:20:30'").build();
        List<Users> users = userRepository.findAll(specification);
        Assertions.assertEquals(2, users.size());
        Assertions.assertFalse(users.stream().anyMatch(u -> "john".equals(u.getUserFirstName())));
    }

    @Test
    void canGetUsersWithUpdatedAtLessThanEqualSearch() {
        userRepository.save(Users.builder().userFirstName("john").updatedAt(LocalDateTime.parse("2020-01-10T10:15:30")).build());
        userRepository.save(Users.builder().userFirstName("robot").updatedAt(LocalDateTime.parse("2020-01-11T10:20:30")).build());
        userRepository.save(Users.builder().userFirstName("robot2").updatedAt(LocalDateTime.parse("2020-01-12T10:20:30")).build());
        Specification<Users> specification = new SpecificationsBuilder<Users>(caseSensitive())
                .withSearch("updatedAt<:'2020-01-11T10:20:30'").build();
        List<Users> users = userRepository.findAll(specification);
        Assertions.assertEquals(2, users.size());
        Assertions.assertFalse(users.stream().anyMatch(u -> "robot2".equals(u.getUserFirstName())));
    }

    @Test
    void canGetUsersWithUpdatedDateAtGreaterSearch() {
        userRepository.save(Users.builder().userFirstName("HamidReza").updatedDateAt(LocalDate.parse("2020-01-10")).build());
        userRepository.save(Users.builder().userFirstName("robot").updatedDateAt(LocalDate.parse("2020-01-11")).build());

        Specification<Users> specification = new SpecificationsBuilder<Users>(caseSensitive())
                .withSearch("updatedDateAt>'2020-01-10'").build();
        List<Users> robotUsers = userRepository.findAll(specification);
        Assertions.assertEquals(1, robotUsers.size());
        Assertions.assertEquals("robot", robotUsers.get(0).getUserFirstName());
    }

    @Test
    void canGetUsersWithUpdatedDateAtLessSearch() {
        userRepository.save(Users.builder().userFirstName("HamidReza").updatedDateAt(LocalDate.parse("2020-01-10")).build());
        userRepository.save(Users.builder().userFirstName("robot").updatedDateAt(LocalDate.parse("2020-01-11")).build());

        Specification<Users> specification = new SpecificationsBuilder<Users>(caseSensitive())
                .withSearch("updatedDateAt<'2020-01-11'").build();
        List<Users> hamidrezaUsers = userRepository.findAll(specification);
        Assertions.assertEquals(1, hamidrezaUsers.size());
        Assertions.assertEquals("HamidReza", hamidrezaUsers.get(0).getUserFirstName());
    }

    @Test
    void canGetUsersWithUpdatedDateAtEqualSearch() {
        userRepository.save(Users.builder().userFirstName("HamidReza").updatedDateAt(LocalDate.parse("2020-01-10")).build());
        userRepository.save(Users.builder().userFirstName("robot").updatedDateAt(LocalDate.parse("2020-01-11")).build());

        Specification<Users> specification = new SpecificationsBuilder<Users>(caseSensitive())
                .withSearch("updatedDateAt:'2020-01-10'").build();
        List<Users> hamidrezaUsers = userRepository.findAll(specification);
        Assertions.assertEquals(1, hamidrezaUsers.size());
        Assertions.assertEquals("HamidReza", hamidrezaUsers.get(0).getUserFirstName());
    }

    @Test
    void canGetUsersWithUpdatedDateAtNotEqualSearch() {
        userRepository.save(Users.builder().userFirstName("HamidReza").updatedDateAt(LocalDate.parse("2020-01-10")).build());
        userRepository.save(Users.builder().userFirstName("robot").updatedDateAt(LocalDate.parse("2020-01-11")).build());

        Specification<Users> specification = new SpecificationsBuilder<Users>(caseSensitive())
                .withSearch("updatedDateAt!'2020-01-11'").build();
        List<Users> hamidrezaUsers = userRepository.findAll(specification);
        Assertions.assertEquals(1, hamidrezaUsers.size());
        Assertions.assertEquals("HamidReza", hamidrezaUsers.get(0).getUserFirstName());
    }

    @Test
    void canGetUsersWithUpdatedDateAtLessThanEqualSearch() {
        userRepository.save(Users.builder().userFirstName("john").updatedDateAt(LocalDate.parse("2020-01-10")).build());
        userRepository.save(Users.builder().userFirstName("robot").updatedDateAt(LocalDate.parse("2020-01-11")).build());
        userRepository.save(Users.builder().userFirstName("robot2").updatedDateAt(LocalDate.parse("2020-01-12")).build());

        Specification<Users> specification = new SpecificationsBuilder<Users>(caseSensitive())
                .withSearch("updatedDateAt<:'2020-01-11'").build();
        List<Users> users = userRepository.findAll(specification);
        Assertions.assertEquals(2, users.size());
        Assertions.assertFalse(users.stream().anyMatch(u -> "robot2".equals(u.getUserFirstName())));
    }

    @Test
    void canGetUsersWithUpdatedDateAtGreaterThanEqualSearch() {
        userRepository.save(Users.builder().userFirstName("john").updatedDateAt(LocalDate.parse("2020-01-10")).build());
        userRepository.save(Users.builder().userFirstName("robot").updatedDateAt(LocalDate.parse("2020-01-11")).build());
        userRepository.save(Users.builder().userFirstName("robot2").updatedDateAt(LocalDate.parse("2020-01-12")).build());

        Specification<Users> specification = new SpecificationsBuilder<Users>(caseSensitive())
                .withSearch("updatedDateAt>:'2020-01-11'").build();
        List<Users> users = userRepository.findAll(specification);
        Assertions.assertEquals(2, users.size());
        Assertions.assertFalse(users.stream().anyMatch(u -> "john".equals(u.getUserFirstName())));
    }

    @Test
    void canGetUsersWithUpdatedTimeAtGreaterSearch() {
        userRepository.save(Users.builder().userFirstName("HamidReza").updatedTimeAt(LocalTime.parse("10:15:30")).build());
        userRepository.save(Users.builder().userFirstName("robot").updatedTimeAt(LocalTime.parse("10:20:30")).build());

        Specification<Users> specification = new SpecificationsBuilder<Users>(caseSensitive())
                .withSearch("updatedTimeAt>'10:15:30'").build();
        List<Users> robotUsers = userRepository.findAll(specification);
        Assertions.assertEquals(1, robotUsers.size());
        Assertions.assertEquals("robot", robotUsers.get(0).getUserFirstName());
    }

    @Test
    void canGetUsersWithUpdatedTimeAtLessSearch() {
        userRepository.save(Users.builder().userFirstName("HamidReza").updatedTimeAt(LocalTime.parse("10:15:30")).build());
        userRepository.save(Users.builder().userFirstName("robot").updatedTimeAt(LocalTime.parse("10:20:30")).build());

        Specification<Users> specification = new SpecificationsBuilder<Users>(caseSensitive())
                .withSearch("updatedTimeAt<'10:16:30'").build();
        List<Users> hamidrezaUsers = userRepository.findAll(specification);
        Assertions.assertEquals(1, hamidrezaUsers.size());
        Assertions.assertEquals("HamidReza", hamidrezaUsers.get(0).getUserFirstName());
    }

    @Test
    void canGetUsersWithUpdatedTimeAtEqualSearch() {
        userRepository.save(Users.builder().userFirstName("HamidReza").updatedTimeAt(LocalTime.parse("10:15:30")).build());
        userRepository.save(Users.builder().userFirstName("robot").updatedTimeAt(LocalTime.parse("10:20:30")).build());

        Specification<Users> specification = new SpecificationsBuilder<Users>(caseSensitive())
                .withSearch("updatedTimeAt:'10:15:30'").build();
        List<Users> hamidrezaUsers = userRepository.findAll(specification);
        Assertions.assertEquals(1, hamidrezaUsers.size());
        Assertions.assertEquals("HamidReza", hamidrezaUsers.get(0).getUserFirstName());
    }

    @Test
    void canGetUsersWithUpdatedTimeAtLessThanEqualSearch() {
        userRepository.save(Users.builder().userFirstName("john").updatedTimeAt(LocalTime.parse("10:15:30")).build());
        userRepository.save(Users.builder().userFirstName("robot").updatedTimeAt(LocalTime.parse("10:20:30")).build());
        userRepository.save(Users.builder().userFirstName("robot2").updatedTimeAt(LocalTime.parse("10:25:30")).build());

        Specification<Users> specification = new SpecificationsBuilder<Users>(caseSensitive())
                .withSearch("updatedTimeAt<:'10:20:30'").build();
        List<Users> users = userRepository.findAll(specification);
        Assertions.assertEquals(2, users.size());
        Assertions.assertFalse(users.stream().anyMatch(u -> "robot2".equals(u.getUserFirstName())));
    }

    @Test
    void canGetUsersWithUpdatedTimeAtGreaterThanEqualSearch() {
        userRepository.save(Users.builder().userFirstName("john").updatedTimeAt(LocalTime.parse("10:15:30")).build());
        userRepository.save(Users.builder().userFirstName("robot").updatedTimeAt(LocalTime.parse("10:20:30")).build());
        userRepository.save(Users.builder().userFirstName("robot2").updatedTimeAt(LocalTime.parse("10:25:30")).build());

        Specification<Users> specification = new SpecificationsBuilder<Users>(caseSensitive())
                .withSearch("updatedTimeAt>:'10:20:30'").build();
        List<Users> users = userRepository.findAll(specification);
        Assertions.assertEquals(2, users.size());
        Assertions.assertFalse(users.stream().anyMatch(u -> "john".equals(u.getUserFirstName())));
    }

    @Test
    void canGetUsersWithUpdatedTimeAtNotEqualSearch() {
        userRepository.save(Users.builder().userFirstName("HamidReza").updatedTimeAt(LocalTime.parse("10:15:30")).build());
        userRepository.save(Users.builder().userFirstName("robot").updatedTimeAt(LocalTime.parse("10:20:30")).build());

        Specification<Users> specification = new SpecificationsBuilder<Users>(caseSensitive())
                .withSearch("updatedTimeAt!'10:20:30'").build();
        List<Users> hamidrezaUsers = userRepository.findAll(specification);
        Assertions.assertEquals(1, hamidrezaUsers.size());
        Assertions.assertEquals("HamidReza", hamidrezaUsers.get(0).getUserFirstName());
    }

    @Test
    void canGetUsersWithDurationGreaterSearch() {
        userRepository.save(Users.builder().userFirstName("HamidReza").validityDuration(Duration.parse("PT10H")).build());
        userRepository.save(Users.builder().userFirstName("robot").validityDuration(Duration.parse("PT15H")).build());

        Specification<Users> specification = new SpecificationsBuilder<Users>(caseSensitive())
                .withSearch("validityDuration>'PT10H'").build();
        List<Users> robotUsers = userRepository.findAll(specification);
        Assertions.assertEquals(1, robotUsers.size());
        Assertions.assertEquals("robot", robotUsers.get(0).getUserFirstName());
    }

    @Test
    void canGetUsersWithDurationLessSearch() {
        userRepository.save(Users.builder().userFirstName("HamidReza").validityDuration(Duration.parse("PT10H")).build());
        userRepository.save(Users.builder().userFirstName("robot").validityDuration(Duration.parse("PT15H")).build());

        Specification<Users> specification = new SpecificationsBuilder<Users>(caseSensitive())
                .withSearch("validityDuration<'PT11H'").build();
        List<Users> hamidrezaUsers = userRepository.findAll(specification);
        Assertions.assertEquals(1, hamidrezaUsers.size());
        Assertions.assertEquals("HamidReza", hamidrezaUsers.get(0).getUserFirstName());
    }

    @Test
    void canGetUsersWithDurationEqualSearch() {
        userRepository.save(Users.builder().userFirstName("HamidReza").validityDuration(Duration.parse("PT10H")).build());
        userRepository.save(Users.builder().userFirstName("robot").validityDuration(Duration.parse("PT15H")).build());

        Specification<Users> specification = new SpecificationsBuilder<Users>(caseSensitive())
                .withSearch("validityDuration:'PT10H'").build();
        List<Users> hamidrezaUsers = userRepository.findAll(specification);
        Assertions.assertEquals(1, hamidrezaUsers.size());
        Assertions.assertEquals("HamidReza", hamidrezaUsers.get(0).getUserFirstName());
    }

    @Test
    void canGetUsersWithDurationNotEqualSearch() {
        userRepository.save(Users.builder().userFirstName("HamidReza").validityDuration(Duration.parse("PT10H")).build());
        userRepository.save(Users.builder().userFirstName("robot").validityDuration(Duration.parse("PT15H")).build());

        Specification<Users> specification = new SpecificationsBuilder<Users>(caseSensitive())
                .withSearch("validityDuration!'PT10H'").build();
        List<Users> robotUsers = userRepository.findAll(specification);
        Assertions.assertEquals(1, robotUsers.size());
        Assertions.assertEquals("robot", robotUsers.get(0).getUserFirstName());
    }

    @Test
    void canGetUsersWithUUIDEqualSearch() {
        UUID userUUID = UUID.randomUUID();
        userRepository.save(Users.builder().userFirstName("Diego").uuid(userUUID).build());
        Specification<Users> specification = new SpecificationsBuilder<Users>(caseSensitive())
                .withSearch("uuid:'" + userUUID + "'").build();
        List<Users> robotUsers = userRepository.findAll(specification);
        Assertions.assertEquals(1, robotUsers.size());
        Assertions.assertEquals(userUUID, robotUsers.get(0).getUuid());
    }

    @Test
    void canGetUsersWithUUIDNotEqualSearch() {
        UUID userUUID = UUID.randomUUID();
        UUID user2UUID = UUID.randomUUID();
        userRepository.save(Users.builder().userFirstName("Diego").uuid(userUUID).build());
        userRepository.save(Users.builder().userFirstName("Diego two").uuid(user2UUID).build());
        Specification<Users> specification = new SpecificationsBuilder<Users>(caseSensitive())
                .withSearch("uuid!'" + userUUID + "'").build();
        List<Users> robotUsers = userRepository.findAll(specification);
        Assertions.assertEquals(1, robotUsers.size());
        Assertions.assertEquals(user2UUID, robotUsers.get(0).getUuid());
    }

    @Test
    void canGetUsersWithNumberOfChildrenLessOrEqualSearch() {
        userRepository.save(Users.builder().userFirstName("john").userChildrenNumber(2).build());
        userRepository.save(Users.builder().userFirstName("jane").userChildrenNumber(3).build());
        userRepository.save(Users.builder().userFirstName("joe").userChildrenNumber(4).build());
        Specification<Users> specification = new SpecificationsBuilder<Users>(caseSensitive())
                .withSearch("userChildrenNumber<:2").build();
        List<Users> users = userRepository.findAll(specification);
        Assertions.assertEquals(1, users.size());
        Assertions.assertEquals("john", users.get(0).getUserFirstName());
    }

    @Test
    void canGetUsersWithNumberOfChildrenGreaterOrEqualSearch() {
        userRepository.save(Users.builder().userFirstName("john").userChildrenNumber(2).build());
        userRepository.save(Users.builder().userFirstName("jane").userChildrenNumber(3).build());
        userRepository.save(Users.builder().userFirstName("joe").userChildrenNumber(4).build());
        Specification<Users> specification = new SpecificationsBuilder<Users>(caseSensitive())
                .withSearch("userChildrenNumber>:3").build();
        List<Users> users = userRepository.findAll(specification);
        Assertions.assertEquals(2, users.size());
    }

    @Test
    void canGetUsersWithNumberOfChildrenLessSearch() {
        userRepository.save(Users.builder().userFirstName("john").userChildrenNumber(2).build());
        userRepository.save(Users.builder().userFirstName("jane").userChildrenNumber(3).build());
        userRepository.save(Users.builder().userFirstName("joe").userChildrenNumber(4).build());
        Specification<Users> specification = new SpecificationsBuilder<Users>(caseSensitive())
                .withSearch("userChildrenNumber<3").build();
        List<Users> users = userRepository.findAll(specification);
        Assertions.assertEquals(1, users.size());
        Assertions.assertEquals("john", users.get(0).getUserFirstName());
    }

    @Test
    void canGetUserWithNameIn() {
        Integer johnId = userRepository.save(Users.builder().userFirstName("john").userChildrenNumber(2).build()).getUserId();
        Integer janeId = userRepository.save(Users.builder().userFirstName("jane").userChildrenNumber(3).build()).getUserId();
        userRepository.save(Users.builder().userFirstName("joe").userChildrenNumber(4).build());
        Specification<Users> specification = new SpecificationsBuilder<Users>(caseSensitive())
                .withSearch("userFirstName IN [\"john\", \"jane\"]").build();
        List<Users> users = userRepository.findAll(specification);
        Assertions.assertTrue(Set.of(johnId, janeId).equals(users.stream().map(Users::getUserId).collect(Collectors.toSet())));
    }

    @Test
    void emptyInArrayThrows() {
        Assertions.assertThrows(SearchQueryException.class, () -> {
            Specification<Users> spec = new SpecificationsBuilder<Users>(caseSensitive())
                    .withSearch("userFirstName IN []").build();
            userRepository.findAll(spec);
        });
    }

    @Test
    void canGetUserWithNameNotIn() {
        userRepository.save(Users.builder().userFirstName("john").userChildrenNumber(2).build());
        userRepository.save(Users.builder().userFirstName("jane").userChildrenNumber(3).build());
        Integer joeId = userRepository.save(Users.builder().userFirstName("joe").userChildrenNumber(4).build()).getUserId();
        Specification<Users> specification = new SpecificationsBuilder<Users>(caseSensitive())
                .withSearch("userFirstName NOT IN [\"john\", \"jane\"]").build();
        List<Users> users = userRepository.findAll(specification);
        Assertions.assertTrue(Set.of(joeId).equals(users.stream().map(Users::getUserId).collect(Collectors.toSet())));
    }

    @Test
    void canGetUserWithChildrenNumberNotIn() {
        userRepository.save(Users.builder().userFirstName("john").userChildrenNumber(2).build());
        userRepository.save(Users.builder().userFirstName("jane").userChildrenNumber(3).build());
        Integer joeId = userRepository.save(Users.builder().userFirstName("joe").userChildrenNumber(4).build()).getUserId();
        Specification<Users> specification = new SpecificationsBuilder<Users>(caseSensitive())
                .withSearch("userChildrenNumber NOT IN [2, 3]").build();
        List<Users> users = userRepository.findAll(specification);
        Assertions.assertTrue(Set.of(joeId).equals(users.stream().map(Users::getUserId).collect(Collectors.toSet())));
    }

    @Test
    void canGetUserWithChildrenNumberIn() {
        Integer johnId = userRepository.save(Users.builder().userFirstName("john").userChildrenNumber(2).build()).getUserId();
        Integer janeId = userRepository.save(Users.builder().userFirstName("jane").userChildrenNumber(3).build()).getUserId();
        userRepository.save(Users.builder().userFirstName("joe").userChildrenNumber(4).build());
        Specification<Users> specification = new SpecificationsBuilder<Users>(caseSensitive())
                .withSearch("userChildrenNumber IN [2, 3]").build();
        List<Users> users = userRepository.findAll(specification);
        Assertions.assertTrue(Set.of(janeId, johnId).equals(users.stream().map(Users::getUserId).collect(Collectors.toSet())));
    }

    @Test
    void canGetUserWithTypeIn() {
        Integer johnId = userRepository.save(Users.builder().userFirstName("john").type(UserType.TEAM_MEMBER).build()).getUserId();
        Integer janeId = userRepository.save(Users.builder().userFirstName("jane").type(UserType.ADMINISTRATOR).build()).getUserId();
        userRepository.save(Users.builder().userFirstName("joe").type(UserType.MANAGER).build());
        Specification<Users> specification = new SpecificationsBuilder<Users>(caseSensitive())
                .withSearch("type IN [ADMINISTRATOR, TEAM_MEMBER]").build();
        List<Users> users = userRepository.findAll(specification);
        Assertions.assertTrue(Set.of(janeId, johnId).equals(users.stream().map(Users::getUserId).collect(Collectors.toSet())));
    }

    @Test
    void canGetUserWithIn() {
        Integer johnId = userRepository.save(Users.builder().userFirstName("john").updatedDateAt(LocalDate.parse("2020-01-10")).build()).getUserId();
        Integer janeId = userRepository.save(Users.builder().userFirstName("jane").updatedDateAt(LocalDate.parse("2020-01-15")).build()).getUserId();
        userRepository.save(Users.builder().userFirstName("joe").updatedDateAt(LocalDate.parse("2021-01-10")).build());
        Specification<Users> specification = new SpecificationsBuilder<Users>(caseSensitive())
                .withSearch("updatedDateAt IN ['2020-01-10', '2020-01-15']").build();
        List<Users> users = userRepository.findAll(specification);
        Assertions.assertTrue(Set.of(janeId, johnId).equals(users.stream().map(Users::getUserId).collect(Collectors.toSet())));
    }

    @Test
    void canGetAuthorsWithEmptyBook() {
        Book johnBook = new Book();
        Author john = new Author();
        john.setName("john");
        john.addBook(johnBook);
        authorRepository.save(john);
        Book janeBook = new Book();
        Author jane = new Author();
        jane.setName("jane");
        jane.addBook(janeBook);
        authorRepository.save(jane);
        Specification<Author> specification = new SpecificationsBuilder<Author>(caseSensitive())
                .withSearch("books IS EMPTY").build();
        List<Author> authors = authorRepository.findAll(specification);
        Assertions.assertTrue(authors.isEmpty());
    }

    @Test
    void cantSearchForEmptyWithNonFieldProperties() {
        Book johnBook = new Book();
        Author john = new Author();
        john.setName("john");
        john.addBook(johnBook);
        authorRepository.save(john);
        Book janeBook = new Book();
        Author jane = new Author();
        jane.setName("jane");
        jane.addBook(janeBook);
        authorRepository.save(jane);
        Specification<Author> specification = new SpecificationsBuilder<Author>(caseSensitive())
                .withSearch("name IS EMPTY").build();
        Assertions.assertThrows(SearchQueryException.class, () -> authorRepository.findAll(specification));
        Specification<Author> specification2 = new SpecificationsBuilder<Author>(caseSensitive())
                .withSearch("name IS NOT EMPTY").build();
        Assertions.assertThrows(SearchQueryException.class, () -> authorRepository.findAll(specification2));
    }

    @Test
    void canGetAuthorsWithEmptyBookWithResult() {
        Book johnBook = new Book();
        Author john = new Author();
        john.setName("john");
        john.addBook(johnBook);
        authorRepository.save(john);
        Author jane = new Author();
        jane.setName("jane");
        authorRepository.save(jane);
        Specification<Author> specification = new SpecificationsBuilder<Author>(caseSensitive())
                .withSearch("books IS EMPTY").build();
        List<Author> authors = authorRepository.findAll(specification);
        Assertions.assertEquals(1, authors.size());
        Assertions.assertEquals(jane.getName(), authors.get(0).getName());
    }

    @Test
    void canGetAuthorsWithBooksNotEmpty() {
        Book johnBook = new Book();
        Author john = new Author();
        john.setName("john");
        john.addBook(johnBook);
        authorRepository.save(john);
        Author jane = new Author();
        jane.setName("jane");
        authorRepository.save(jane);
        Specification<Author> specification = new SpecificationsBuilder<Author>(caseSensitive())
                .withSearch("books IS NOT EMPTY").build();
        List<Author> authors = authorRepository.findAll(specification);
        Assertions.assertEquals(1, authors.size());
        Assertions.assertEquals(john.getName(), authors.get(0).getName());
    }

    @Test
    void canGetAuthorsWithBooksNotEmptyAllResult() {
        Book johnBook = new Book();
        Author john = new Author();
        john.setName("john");
        john.addBook(johnBook);
        authorRepository.save(john);
        Author jane = new Author();
        jane.setName("jane");
        Book janeBook = new Book();
        jane.addBook(janeBook);
        authorRepository.save(jane);
        Specification<Author> specification = new SpecificationsBuilder<Author>(caseSensitive())
                .withSearch("books IS NOT EMPTY").build();
        List<Author> authors = authorRepository.findAll(specification);
        Assertions.assertEquals(2, authors.size());
    }

    @Test
    void canGetAuthorsWithBooksNotEmptyNoResult() {
        Author john = new Author();
        john.setName("john");
        authorRepository.save(john);
        Author jane = new Author();
        jane.setName("jane");
        authorRepository.save(jane);
        Specification<Author> specification = new SpecificationsBuilder<Author>(caseSensitive())
                .withSearch("books IS NOT EMPTY").build();
        List<Author> authors = authorRepository.findAll(specification);
        Assertions.assertEquals(0, authors.size());
    }

    @Test
    void canGetAuthorsWithBooksNull() {
        Author john = new Author();
        john.setName("john");
        authorRepository.save(john);
        Author jane = new Author();
        jane.setName("jane");
        authorRepository.save(jane);
        Specification<Author> spec = new SpecificationsBuilder<Author>(caseSensitive())
                .withSearch("books IS NULL").build();
        Assertions.assertThrows(UnsupportedOperationException.class, () -> authorRepository.findAll(spec));

        Specification<Author> specNotNull = new SpecificationsBuilder<Author>(caseSensitive())
                .withSearch("books IS NOT NULL").build();
        Assertions.assertThrows(UnsupportedOperationException.class, () -> authorRepository.findAll(specNotNull));
    }

    @Test
    void canGetUsersWithNumberOfChildrenBetween() {
        userRepository.save(Users.builder().userFirstName("john").userChildrenNumber(2).build());
        userRepository.save(Users.builder().userFirstName("jane").userChildrenNumber(3).build());
        userRepository.save(Users.builder().userFirstName("joe").userChildrenNumber(5).build());
        userRepository.save(Users.builder().userFirstName("jean").userChildrenNumber(10).build());
        Specification<Users> specification = new SpecificationsBuilder<Users>(caseSensitive())
                .withSearch("userChildrenNumber BETWEEN 4 AND 10").build();
        List<Users> users = userRepository.findAll(specification);
        Assertions.assertEquals(2, users.size());
        Set<String> setNames = users.stream().map(Users::getUserFirstName).collect(Collectors.toSet());
        Assertions.assertEquals(Set.of("joe", "jean"), setNames);
    }

    @Test
    void canGetUsersWithUpdatedDateBetween() {
        userRepository.save(Users.builder().userFirstName("john").updatedDateAt(LocalDate.parse("2020-01-10")).build());
        userRepository.save(Users.builder().userFirstName("jane").updatedDateAt(LocalDate.parse("2020-01-11")).build());
        userRepository.save(Users.builder().userFirstName("joe").updatedDateAt(LocalDate.parse("2020-01-12")).build());
        userRepository.save(Users.builder().userFirstName("jean").updatedDateAt(LocalDate.parse("2020-01-13")).build());
        Specification<Users> specification = new SpecificationsBuilder<Users>(caseSensitive())
                .withSearch("updatedDateAt BETWEEN 2020-01-12 AND 2020-01-13").build();
        List<Users> users = userRepository.findAll(specification);
        Assertions.assertEquals(2, users.size());
        Set<String> setNames = users.stream().map(Users::getUserFirstName).collect(Collectors.toSet());
        Assertions.assertEquals(Set.of("joe", "jean"), setNames);
    }

    @Test
    void canGetUsersWithUpdatedDateNotBetween() {
        userRepository.save(Users.builder().userFirstName("john").updatedDateAt(LocalDate.parse("2020-01-10")).build());
        userRepository.save(Users.builder().userFirstName("jane").updatedDateAt(LocalDate.parse("2020-01-11")).build());
        userRepository.save(Users.builder().userFirstName("joe").updatedDateAt(LocalDate.parse("2020-01-12")).build());
        userRepository.save(Users.builder().userFirstName("jean").updatedDateAt(LocalDate.parse("2020-01-13")).build());
        Specification<Users> specification = new SpecificationsBuilder<Users>(caseSensitive())
                .withSearch("updatedDateAt NOT BETWEEN 2020-01-12 AND 2020-01-13").build();
        List<Users> users = userRepository.findAll(specification);
        Assertions.assertEquals(2, users.size());
        Set<String> setNames = users.stream().map(Users::getUserFirstName).collect(Collectors.toSet());
        Assertions.assertEquals(Set.of("john", "jane"), setNames);
    }

    @Test
    void canGetUsersWithUpdatedDateBetweenAndIdIn() {
        userRepository.save(Users.builder().userFirstName("john").updatedDateAt(LocalDate.parse("2020-01-10")).build());
        userRepository.save(Users.builder().userFirstName("jane").updatedDateAt(LocalDate.parse("2020-01-11")).build());
        Integer joeId = userRepository.save(Users.builder().userFirstName("joe").updatedDateAt(LocalDate.parse("2020-01-12")).build()).getUserId();
        Integer jeanId = userRepository.save(Users.builder().userFirstName("jean").updatedDateAt(LocalDate.parse("2020-01-13")).build()).getUserId();
        Specification<Users> specification = new SpecificationsBuilder<Users>(caseSensitive())
                .withSearch("updatedDateAt BETWEEN 2020-01-11 AND 2020-01-13 AND userId IN [" + joeId + ", " + jeanId + "]").build();
        List<Users> users = userRepository.findAll(specification);
        Assertions.assertEquals(2, users.size());
        Set<String> setNames = users.stream().map(Users::getUserFirstName).collect(Collectors.toSet());
        Assertions.assertEquals(Set.of("joe", "jean"), setNames);
    }

    @Test
    void canGetUsersWithUserFirstNameBetween() {
        userRepository.save(Users.builder().userFirstName("abel").build());
        userRepository.save(Users.builder().userFirstName("bob").build());
        userRepository.save(Users.builder().userFirstName("connor").build());
        userRepository.save(Users.builder().userFirstName("david").build());
        Specification<Users> specification = new SpecificationsBuilder<Users>(caseSensitive())
                .withSearch("userFirstName BETWEEN 'aaron' AND 'cyrano'").build();
        List<Users> users = userRepository.findAll(specification);
        Assertions.assertEquals(3, users.size());
        Set<String> setNames = users.stream().map(Users::getUserFirstName).collect(Collectors.toSet());
        Assertions.assertEquals(Set.of("abel", "bob", "connor"), setNames);
    }

    @Test
    void canGetUsersWithUserFirstNameNotBetween() {
        userRepository.save(Users.builder().userFirstName("abel").build());
        userRepository.save(Users.builder().userFirstName("bob").build());
        userRepository.save(Users.builder().userFirstName("connor").build());
        userRepository.save(Users.builder().userFirstName("david").build());
        Specification<Users> specification = new SpecificationsBuilder<Users>(caseSensitive())
                .withSearch("userFirstName NOT BETWEEN 'aaron' AND 'cyrano'").build();
        List<Users> users = userRepository.findAll(specification);
        Assertions.assertEquals(1, users.size());
        Set<String> setNames = users.stream().map(Users::getUserFirstName).collect(Collectors.toSet());
        Assertions.assertEquals(Set.of("david"), setNames);
    }

    @Test
    void canGetUsersWithUserFirstNameGt() {
        userRepository.save(Users.builder().userFirstName("abel").build());
        userRepository.save(Users.builder().userFirstName("bob").build());
        userRepository.save(Users.builder().userFirstName("connor").build());
        userRepository.save(Users.builder().userFirstName("david").build());
        Specification<Users> specification = new SpecificationsBuilder<Users>(caseSensitive())
                .withSearch("userFirstName > barry'").build();
        List<Users> users = userRepository.findAll(specification);
        Assertions.assertEquals(3, users.size());
        Set<String> setNames = users.stream().map(Users::getUserFirstName).collect(Collectors.toSet());
        Assertions.assertEquals(Set.of("connor", "david", "bob"), setNames);
    }

    @Test
    void canGetUsersWithUserFirstNameLt() {
        userRepository.save(Users.builder().userFirstName("abel").build());
        userRepository.save(Users.builder().userFirstName("bob").build());
        userRepository.save(Users.builder().userFirstName("connor").build());
        userRepository.save(Users.builder().userFirstName("david").build());
        Specification<Users> specification = new SpecificationsBuilder<Users>(caseSensitive())
                .withSearch("userFirstName < barry'").build();
        List<Users> users = userRepository.findAll(specification);
        Assertions.assertEquals(1, users.size());
        Set<String> setNames = users.stream().map(Users::getUserFirstName).collect(Collectors.toSet());
        Assertions.assertEquals(Set.of("abel"), setNames);
    }

    @Test
    void canGetUsersWithUserFirstNameCaseSensitive() {
        userRepository.save(Users.builder().userFirstName("abel").build());
        userRepository.save(Users.builder().userFirstName("Aaron").build());
        userRepository.save(Users.builder().userFirstName("connor").build());
        userRepository.save(Users.builder().userFirstName("david").build());
        Specification<Users> specification = new SpecificationsBuilder<Users>(caseSensitive())
                .withSearch("userFirstName : A*").build();
        List<Users> users = userRepository.findAll(specification);
        Assertions.assertEquals(1, users.size());
        Assertions.assertEquals("Aaron", users.get(0).getUserFirstName());
    }

    @Test
    void badRequestWithWrongSearch() {
        Assertions.assertThrows(SearchQueryException.class, () ->
                new SpecificationsBuilder<Users>(caseSensitive()).withSearch("userFirstName : ").build());
        Assertions.assertThrows(SearchQueryException.class, () ->
                new SpecificationsBuilder<Users>(caseSensitive()).withSearch("updatedDateAt BETWEEN  AND 2020-01-11").build());
        Assertions.assertThrows(SearchQueryException.class, () ->
                new SpecificationsBuilder<Users>(caseSensitive()).withSearch("updatedDateAt BETWEEN 2020-01-11 AND").build());
        Assertions.assertThrows(SearchQueryException.class, () ->
                new SpecificationsBuilder<Author>(caseSensitive()).withSearch("books IS EMPT ").build());
        Assertions.assertThrows(SearchQueryException.class, () ->
                new SpecificationsBuilder<Author>(caseSensitive()).withSearch("books IS NOT EMPT ").build());
        Assertions.assertThrows(SearchQueryException.class, () ->
                new SpecificationsBuilder<Users>(caseSensitive()).withSearch("userId IN [").build());
    }

    @Test
    void canGetUsersWithNullColumn() {
        userRepository.save(Users.builder().userFirstName("john").type(null).build());
        userRepository.save(Users.builder().userFirstName("jane").type(UserType.ADMINISTRATOR).build());
        userRepository.save(Users.builder().userFirstName("joe").type(UserType.MANAGER).build());
        userRepository.save(Users.builder().userFirstName("jean").type(null).build());
        Specification<Users> specification = new SpecificationsBuilder<Users>(caseSensitive())
                .withSearch("type IS NULL").build();
        List<Users> users = userRepository.findAll(specification);
        Assertions.assertEquals(2, users.size());
        Set<String> setNames = users.stream().map(Users::getUserFirstName).collect(Collectors.toSet());
        Assertions.assertEquals(Set.of("john", "jean"), setNames);
    }

    @Test
    void canGetUsersWithNotNullColumn() {
        userRepository.save(Users.builder().userFirstName("john").type(null).build());
        userRepository.save(Users.builder().userFirstName("jane").type(UserType.ADMINISTRATOR).build());
        userRepository.save(Users.builder().userFirstName("joe").type(UserType.MANAGER).build());
        userRepository.save(Users.builder().userFirstName("jean").type(null).build());
        Specification<Users> specification = new SpecificationsBuilder<Users>(caseSensitive())
                .withSearch("type IS NOT NULL").build();
        List<Users> users = userRepository.findAll(specification);
        Assertions.assertEquals(2, users.size());
        Set<String> setNames = users.stream().map(Users::getUserFirstName).collect(Collectors.toSet());
        Assertions.assertEquals(Set.of("jane", "joe"), setNames);
    }

    @Test
    void canGetUsersWithNotNullFirstName() {
        userRepository.save(Users.builder().userFirstName("john").type(null).build());
        userRepository.save(Users.builder().userFirstName("jane").type(UserType.ADMINISTRATOR).build());
        userRepository.save(Users.builder().userFirstName("joe").type(UserType.MANAGER).build());
        userRepository.save(Users.builder().userFirstName("jean").type(null).build());
        Specification<Users> specification = new SpecificationsBuilder<Users>(caseSensitive())
                .withSearch("userFirstName IS NOT NULL").build();
        List<Users> users = userRepository.findAll(specification);
        Assertions.assertEquals(4, users.size());
        Set<String> setNames = users.stream().map(Users::getUserFirstName).collect(Collectors.toSet());
        Assertions.assertEquals(Set.of("john", "jane", "joe", "jean"), setNames);
    }

    @Test
    void canGetUserWithNullSalary() {
        userRepository.save(Users.builder().userFirstName("john").userSalary(100.0F).build());
        userRepository.save(Users.builder().userFirstName("jane").userSalary(1000.0F).build());
        Specification<Users> specification = new SpecificationsBuilder<Users>(caseInsensitive())
                .withSearch("userSalary IS NULL").build();
        List<Users> users = userRepository.findAll(specification);
        Assertions.assertEquals(0, users.size());
    }

    @Test
    void canNotSearchABlackListedField() {
        Assertions.assertThrows(SearchQueryException.class, () ->
                new SpecificationsBuilder<Users>(withBlackList())
                        .withSearch("userFirstName : A* AND userId : 3").build());
    }

    @Test
    void canGetUsersWithUUIDNull() {
        UUID userUUID = UUID.randomUUID();
        userRepository.save(Users.builder().userFirstName("Diego").uuid(userUUID).build());
        userRepository.save(Users.builder().userFirstName("Diego two").uuid(null).build());
        Specification<Users> specification = new SpecificationsBuilder<Users>(caseInsensitive())
                .withSearch("uuid IS NULL").build();
        List<Users> robotUsers = userRepository.findAll(specification);
        Assertions.assertEquals(1, robotUsers.size());
        Assertions.assertNull(robotUsers.get(0).getUuid());
    }

    @Test
    void canGetUsersWithUpdatedDateAtNull() {
        userRepository.save(Users.builder().userFirstName("john").updatedDateAt(LocalDate.parse("2020-01-10")).build());
        userRepository.save(Users.builder().userFirstName("jane").updatedDateAt(LocalDate.parse("2020-01-11")).build());
        userRepository.save(Users.builder().userFirstName("joe").updatedDateAt(null).build());
        userRepository.save(Users.builder().userFirstName("jean").updatedDateAt(null).build());
        Specification<Users> specification = new SpecificationsBuilder<Users>(caseInsensitive())
                .withSearch("updatedDateAt IS NULL").build();
        List<Users> users = userRepository.findAll(specification);
        Assertions.assertEquals(2, users.size());
        Set<String> setNames = users.stream().map(Users::getUserFirstName).collect(Collectors.toSet());
        Assertions.assertEquals(Set.of("joe", "jean"), setNames);
    }

    @Test
    void canGetUsersWithUpdatedDateTimeAtNotNull() {
        userRepository.save(Users.builder().userFirstName("john").updatedAt(LocalDateTime.parse("2020-01-10T10:15:30")).build());
        userRepository.save(Users.builder().userFirstName("jane").updatedAt(LocalDateTime.parse("2020-01-11T10:15:30")).build());
        userRepository.save(Users.builder().userFirstName("joe").updatedAt(null).build());
        userRepository.save(Users.builder().userFirstName("jean").updatedAt(LocalDateTime.parse("2020-01-13T10:15:30")).build());
        Specification<Users> specification = new SpecificationsBuilder<Users>(caseInsensitive())
                .withSearch("updatedAt IS NOT NULL").build();
        List<Users> users = userRepository.findAll(specification);
        Assertions.assertEquals(3, users.size());
        Set<String> setNames = users.stream().map(Users::getUserFirstName).collect(Collectors.toSet());
        Assertions.assertEquals(Set.of("john", "jane", "jean"), setNames);
    }

    @Test
    void canGetUsersWithActiveNull() {
        userRepository.save(Users.builder().userFirstName("john").active(true).build());
        userRepository.save(Users.builder().userFirstName("jane").active(false).build());
        userRepository.save(Users.builder().userFirstName("joe").active(null).build());
        userRepository.save(Users.builder().userFirstName("jean").active(null).build());
        Specification<Users> specification = new SpecificationsBuilder<Users>(caseInsensitive())
                .withSearch("active IS NULL").build();
        List<Users> users = userRepository.findAll(specification);
        Assertions.assertEquals(2, users.size());
        Set<String> setNames = users.stream().map(Users::getUserFirstName).collect(Collectors.toSet());
        Assertions.assertEquals(Set.of("joe", "jean"), setNames);
    }

    @Test
    void canGetUsersWithUserChildrenNumberNull() {
        userRepository.save(Users.builder().userFirstName("john").userChildrenNumber(2).build());
        userRepository.save(Users.builder().userFirstName("jane").userChildrenNumber(3).build());
        userRepository.save(Users.builder().userFirstName("joe").userChildrenNumber(null).build());
        userRepository.save(Users.builder().userFirstName("jean").userChildrenNumber(null).build());
        Specification<Users> specification = new SpecificationsBuilder<Users>(caseInsensitive())
                .withSearch("userChildrenNumber IS NULL").build();
        List<Users> users = userRepository.findAll(specification);
        Assertions.assertEquals(2, users.size());
        Set<String> setNames = users.stream().map(Users::getUserFirstName).collect(Collectors.toSet());
        Assertions.assertEquals(Set.of("joe", "jean"), setNames);
    }

    @Test
    void canGetUsersWithCreatedAtNull() {
        userRepository.save(Users.builder().userFirstName("john").createdAt(new Date()).build());
        userRepository.save(Users.builder().userFirstName("jane").createdAt(new Date()).build());
        userRepository.save(Users.builder().userFirstName("joe").createdAt(null).build());
        userRepository.save(Users.builder().userFirstName("jean").createdAt(null).build());
        Specification<Users> specification = new SpecificationsBuilder<Users>(caseInsensitive())
                .withSearch("createdAt IS NULL").build();
        List<Users> users = userRepository.findAll(specification);
        Assertions.assertEquals(2, users.size());
        Set<String> setNames = users.stream().map(Users::getUserFirstName).collect(Collectors.toSet());
        Assertions.assertEquals(Set.of("joe", "jean"), setNames);
    }

    @Test
    void canGetUsersWithEnumCaseInsensitive() {
        userRepository.save(Users.builder().userFirstName("john").type(UserType.ADMINISTRATOR).build());
        userRepository.save(Users.builder().userFirstName("jane").type(UserType.MANAGER).build());
        Specification<Users> specification = new SpecificationsBuilder<Users>(caseSensitive())
                .withSearch("type:administrator").build();
        List<Users> users = userRepository.findAll(specification);
        Assertions.assertEquals(1, users.size());
        Assertions.assertEquals("john", users.get(0).getUserFirstName());
    }

    @Test
    void invalidBooleanValueThrows() {
        Assertions.assertThrows(SearchQueryException.class, () -> {
            Specification<Users> spec = new SpecificationsBuilder<Users>(caseSensitive())
                    .withSearch("isAdmin:banana").build();
            userRepository.findAll(spec);
        });
    }

    @Test
    void nestedPathBlacklistPreventsAccess() {
        @SuppressWarnings("unchecked")
        SearchSpec blackListAuthor = (SearchSpec) Proxy.newProxyInstance(
                SearchSpec.class.getClassLoader(),
                new Class[]{SearchSpec.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "searchParam" -> "";
                    case "caseSensitiveFlag" -> true;
                    case "blackListedFields" -> new String[]{"author"};
                    case "whiteListedFields" -> new String[]{};
                    case "required" -> false;
                    case "defaultValue" -> "";
                    case "maxLength" -> 1024;
                    case "annotationType" -> SearchSpec.class;
                    default -> method.getDefaultValue();
                }
        );
        Assertions.assertThrows(SearchQueryException.class, () ->
                new SpecificationsBuilder<Book>(blackListAuthor)
                        .withSearch("author.name:john").build());
    }

    @Test
    void unsupportedOperationOnFieldThrows() {
        Assertions.assertThrows(SearchQueryException.class, () -> {
            Specification<Users> spec = new SpecificationsBuilder<Users>(caseSensitive())
                    .withSearch("isAdmin IS EMPTY").build();
            userRepository.findAll(spec);
        });
    }

    @Test
    void canGetUserWithCaseInsensitiveLessThan() {
        userRepository.save(Users.builder().userFirstName("abel").build());
        userRepository.save(Users.builder().userFirstName("Bob").build());
        userRepository.save(Users.builder().userFirstName("charlie").build());
        Specification<Users> specification = new SpecificationsBuilder<Users>(caseInsensitive())
                .withSearch("userFirstName<bob").build();
        List<Users> users = userRepository.findAll(specification);
        Assertions.assertEquals(1, users.size());
        Assertions.assertEquals("abel", users.get(0).getUserFirstName());
    }

    @Test
    void canGetUserWithCaseInsensitiveLessThanEquals() {
        userRepository.save(Users.builder().userFirstName("abel").build());
        userRepository.save(Users.builder().userFirstName("Bob").build());
        userRepository.save(Users.builder().userFirstName("charlie").build());
        Specification<Users> specification = new SpecificationsBuilder<Users>(caseInsensitive())
                .withSearch("userFirstName<:bob").build();
        List<Users> users = userRepository.findAll(specification);
        Assertions.assertEquals(2, users.size());
    }

    @Test
    void invalidEnumValueThrows() {
        Assertions.assertThrows(SearchQueryException.class, () -> {
            Specification<Users> spec = new SpecificationsBuilder<Users>(caseSensitive())
                    .withSearch("type:NONEXISTENT").build();
            userRepository.findAll(spec);
        });
    }

    @Test
    void wildcardCharactersAreEscapedInLike() {
        userRepository.save(Users.builder().userFirstName("100%_done").build());
        userRepository.save(Users.builder().userFirstName("normal").build());
        Specification<Users> specification = new SpecificationsBuilder<Users>(caseSensitive())
                .withSearch("userFirstName:*%*").build();
        List<Users> users = userRepository.findAll(specification);
        Assertions.assertEquals(1, users.size());
        Assertions.assertEquals("100%_done", users.get(0).getUserFirstName());
    }

    @Test
    void wildcardUnderscoreIsEscapedInLike() {
        userRepository.save(Users.builder().userFirstName("hello_world").build());
        userRepository.save(Users.builder().userFirstName("helloXworld").build());
        Specification<Users> specification = new SpecificationsBuilder<Users>(caseSensitive())
                .withSearch("userFirstName:*_*").build();
        List<Users> users = userRepository.findAll(specification);
        Assertions.assertEquals(1, users.size());
        Assertions.assertEquals("hello_world", users.get(0).getUserFirstName());
    }

    @Test
    void caseInsensitiveWildcardSearch() {
        userRepository.save(Users.builder().userFirstName("100%_done").build());
        userRepository.save(Users.builder().userFirstName("normal").build());
        Specification<Users> specification = new SpecificationsBuilder<Users>(caseInsensitive())
                .withSearch("userFirstName:*%*").build();
        List<Users> users = userRepository.findAll(specification);
        Assertions.assertEquals(1, users.size());
    }

    @Test
    void caseInsensitiveDoesntContain() {
        userRepository.save(Users.builder().userFirstName("hello").build());
        userRepository.save(Users.builder().userFirstName("world").build());
        Specification<Users> specification = new SpecificationsBuilder<Users>(caseInsensitive())
                .withSearch("userFirstName!*ELL*").build();
        List<Users> users = userRepository.findAll(specification);
        Assertions.assertEquals(1, users.size());
        Assertions.assertEquals("world", users.get(0).getUserFirstName());
    }

    @Test
    void caseInsensitiveStartsWith() {
        userRepository.save(Users.builder().userFirstName("Hello").build());
        userRepository.save(Users.builder().userFirstName("world").build());
        Specification<Users> specification = new SpecificationsBuilder<Users>(caseInsensitive())
                .withSearch("userFirstName:hel*").build();
        List<Users> users = userRepository.findAll(specification);
        Assertions.assertEquals(1, users.size());
        Assertions.assertEquals("Hello", users.get(0).getUserFirstName());
    }

    @Test
    void caseInsensitiveDoesntStartWith() {
        userRepository.save(Users.builder().userFirstName("Hello").build());
        userRepository.save(Users.builder().userFirstName("world").build());
        Specification<Users> specification = new SpecificationsBuilder<Users>(caseInsensitive())
                .withSearch("userFirstName!hel*").build();
        List<Users> users = userRepository.findAll(specification);
        Assertions.assertEquals(1, users.size());
        Assertions.assertEquals("world", users.get(0).getUserFirstName());
    }

    @Test
    void caseInsensitiveEndsWith() {
        userRepository.save(Users.builder().userFirstName("Hello").build());
        userRepository.save(Users.builder().userFirstName("world").build());
        Specification<Users> specification = new SpecificationsBuilder<Users>(caseInsensitive())
                .withSearch("userFirstName:*LLO").build();
        List<Users> users = userRepository.findAll(specification);
        Assertions.assertEquals(1, users.size());
        Assertions.assertEquals("Hello", users.get(0).getUserFirstName());
    }

    @Test
    void caseInsensitiveDoesntEndWith() {
        userRepository.save(Users.builder().userFirstName("Hello").build());
        userRepository.save(Users.builder().userFirstName("world").build());
        Specification<Users> specification = new SpecificationsBuilder<Users>(caseInsensitive())
                .withSearch("userFirstName!*LLO").build();
        List<Users> users = userRepository.findAll(specification);
        Assertions.assertEquals(1, users.size());
        Assertions.assertEquals("world", users.get(0).getUserFirstName());
    }

    @Test
    void canSearchDoubleGreaterThan() {
        userRepository.save(Users.builder().userFirstName("john").userAgeInSeconds(100.0).build());
        userRepository.save(Users.builder().userFirstName("jane").userAgeInSeconds(200.0).build());
        Specification<Users> specification = new SpecificationsBuilder<Users>(caseSensitive())
                .withSearch("userAgeInSeconds>150").build();
        List<Users> users = userRepository.findAll(specification);
        Assertions.assertEquals(1, users.size());
        Assertions.assertEquals("jane", users.get(0).getUserFirstName());
    }

    @Test
    void canSearchFloatLessThan() {
        userRepository.save(Users.builder().userFirstName("john").userSalary(100.0F).build());
        userRepository.save(Users.builder().userFirstName("jane").userSalary(200.0F).build());
        Specification<Users> specification = new SpecificationsBuilder<Users>(caseSensitive())
                .withSearch("userSalary<150").build();
        List<Users> users = userRepository.findAll(specification);
        Assertions.assertEquals(1, users.size());
        Assertions.assertEquals("john", users.get(0).getUserFirstName());
    }

    @Test
    void canSearchDurationGreaterThan() {
        userRepository.save(Users.builder().userFirstName("john").validityDuration(Duration.ofDays(10)).build());
        userRepository.save(Users.builder().userFirstName("jane").validityDuration(Duration.ofDays(60)).build());
        Specification<Users> specification = new SpecificationsBuilder<Users>(caseSensitive())
                .withSearch("validityDuration>PT720H").build();
        List<Users> users = userRepository.findAll(specification);
        Assertions.assertEquals(1, users.size());
        Assertions.assertEquals("jane", users.get(0).getUserFirstName());
    }

    @Test
    void canSearchInstantLessThan() {
        Instant past = Instant.parse("2020-01-01T00:00:00Z");
        Instant future = Instant.parse("2025-01-01T00:00:00Z");
        userRepository.save(Users.builder().userFirstName("john").updatedInstantAt(past).build());
        userRepository.save(Users.builder().userFirstName("jane").updatedInstantAt(future).build());
        Specification<Users> specification = new SpecificationsBuilder<Users>(caseSensitive())
                .withSearch("updatedInstantAt<'2023-01-01T00:00:00Z'").build();
        List<Users> users = userRepository.findAll(specification);
        Assertions.assertEquals(1, users.size());
        Assertions.assertEquals("john", users.get(0).getUserFirstName());
    }

    @Test
    void canSearchLocalTimeGreaterThan() {
        userRepository.save(Users.builder().userFirstName("john").updatedTimeAt(LocalTime.of(8, 0)).build());
        userRepository.save(Users.builder().userFirstName("jane").updatedTimeAt(LocalTime.of(18, 0)).build());
        Specification<Users> specification = new SpecificationsBuilder<Users>(caseSensitive())
                .withSearch("updatedTimeAt>'12:00'").build();
        List<Users> users = userRepository.findAll(specification);
        Assertions.assertEquals(1, users.size());
        Assertions.assertEquals("jane", users.get(0).getUserFirstName());
    }

    @Test
    void isNotNullOperation() {
        userRepository.save(Users.builder().userFirstName("john").userLastName(null).build());
        userRepository.save(Users.builder().userFirstName("jane").userLastName("Smith").build());
        Specification<Users> specification = new SpecificationsBuilder<Users>(caseSensitive())
                .withSearch("userLastName IS NOT NULL").build();
        List<Users> users = userRepository.findAll(specification);
        Assertions.assertEquals(1, users.size());
        Assertions.assertEquals("jane", users.get(0).getUserFirstName());
    }

    @Test
    void notInArrayOperation() {
        userRepository.save(Users.builder().userFirstName("john").build());
        userRepository.save(Users.builder().userFirstName("jane").build());
        userRepository.save(Users.builder().userFirstName("bob").build());
        Specification<Users> specification = new SpecificationsBuilder<Users>(caseSensitive())
                .withSearch("userFirstName NOT IN [john,jane]").build();
        List<Users> users = userRepository.findAll(specification);
        Assertions.assertEquals(1, users.size());
        Assertions.assertEquals("bob", users.get(0).getUserFirstName());
    }

    @Test
    void notBetweenOperation() {
        userRepository.save(Users.builder().userFirstName("john").userChildrenNumber(5).build());
        userRepository.save(Users.builder().userFirstName("jane").userChildrenNumber(15).build());
        userRepository.save(Users.builder().userFirstName("bob").userChildrenNumber(25).build());
        Specification<Users> specification = new SpecificationsBuilder<Users>(caseSensitive())
                .withSearch("userChildrenNumber NOT BETWEEN 10 AND 20").build();
        List<Users> users = userRepository.findAll(specification);
        Assertions.assertEquals(2, users.size());
    }

    @Test
    void betweenWithQuotedStrings() {
        userRepository.save(Users.builder().userFirstName("john").userChildrenNumber(5).build());
        userRepository.save(Users.builder().userFirstName("jane").userChildrenNumber(15).build());
        Specification<Users> specification = new SpecificationsBuilder<Users>(caseSensitive())
                .withSearch("userChildrenNumber BETWEEN '5' AND '15'").build();
        List<Users> users = userRepository.findAll(specification);
        Assertions.assertEquals(2, users.size());
    }

    @Test
    void inArrayWithQuotedStrings() {
        userRepository.save(Users.builder().userFirstName("john").build());
        userRepository.save(Users.builder().userFirstName("jane").build());
        userRepository.save(Users.builder().userFirstName("bob").build());
        Specification<Users> specification = new SpecificationsBuilder<Users>(caseSensitive())
                .withSearch("userFirstName IN ['john','jane']").build();
        List<Users> users = userRepository.findAll(specification);
        Assertions.assertEquals(2, users.size());
    }

    @Test
    void doubleQuotedStringInSearch() {
        userRepository.save(Users.builder().userFirstName("john doe").build());
        userRepository.save(Users.builder().userFirstName("jane").build());
        Specification<Users> specification = new SpecificationsBuilder<Users>(caseSensitive())
                .withSearch("userFirstName:\"john doe\"").build();
        List<Users> users = userRepository.findAll(specification);
        Assertions.assertEquals(1, users.size());
        Assertions.assertEquals("john doe", users.get(0).getUserFirstName());
    }

    @Test
    void escapedCharactersInStringValue() {
        userRepository.save(Users.builder().userFirstName("tab\there").build());
        userRepository.save(Users.builder().userFirstName("normal").build());
        Specification<Users> specification = new SpecificationsBuilder<Users>(caseSensitive())
                .withSearch("userFirstName:'tab\\there'").build();
        List<Users> users = userRepository.findAll(specification);
        Assertions.assertEquals(1, users.size());
    }

    @Test
    void escapedDoubleQuoteInStringValue() {
        userRepository.save(Users.builder().userFirstName("say\"hello").build());
        userRepository.save(Users.builder().userFirstName("normal").build());
        Specification<Users> specification = new SpecificationsBuilder<Users>(caseSensitive())
                .withSearch("userFirstName:'say\\\"hello'").build();
        List<Users> users = userRepository.findAll(specification);
        Assertions.assertEquals(1, users.size());
    }

    @Test
    void escapedBackslashInStringValue() {
        userRepository.save(Users.builder().userFirstName("back\\slash").build());
        userRepository.save(Users.builder().userFirstName("normal").build());
        Specification<Users> specification = new SpecificationsBuilder<Users>(caseSensitive())
                .withSearch("userFirstName:'back\\\\slash'").build();
        List<Users> users = userRepository.findAll(specification);
        Assertions.assertEquals(1, users.size());
    }

    @Test
    void escapedQuoteInStringValue() {
        userRepository.save(Users.builder().userFirstName("it's").build());
        userRepository.save(Users.builder().userFirstName("normal").build());
        Specification<Users> specification = new SpecificationsBuilder<Users>(caseSensitive())
                .withSearch("userFirstName:\"it\\'s\"").build();
        List<Users> users = userRepository.findAll(specification);
        Assertions.assertEquals(1, users.size());
    }

    @Test
    void whiteListAllowsRootOfNestedPath() {
        userRepository.save(Users.builder().userFirstName("john").build());
        Specification<Users> specification = new SpecificationsBuilder<Users>(
                createAnnotation(true, new String[]{"userFirstName", "userLastName"}, new String[]{}))
                .withSearch("userFirstName:john").build();
        List<Users> users = userRepository.findAll(specification);
        Assertions.assertFalse(users.isEmpty());
    }

    @Test
    void whiteListRejectsUnlistedField() {
        Assertions.assertThrows(SearchQueryException.class, () -> {
            Specification<Users> spec = new SpecificationsBuilder<Users>(
                    createAnnotation(true, new String[]{"userFirstName"}, new String[]{}))
                    .withSearch("userLastName:doe").build();
            userRepository.findAll(spec);
        });
    }

    @Test
    void dateSearchWithIsoDateTimeFormat() throws ParseException {
        SimpleDateFormat sdf = newDateFormat();
        sdf.applyPattern("yyyy-MM-dd'T'HH:mm:ss");
        Date d = sdf.parse("2020-06-15T10:30:00");
        userRepository.save(Users.builder().userFirstName("john").createdAt(d).build());
        userRepository.save(Users.builder().userFirstName("jane").createdAt(new Date()).build());
        Specification<Users> specification = new SpecificationsBuilder<Users>(caseSensitive())
                .withSearch("createdAt:'2020-06-15T10:30:00'").build();
        List<Users> users = userRepository.findAll(specification);
        Assertions.assertEquals(1, users.size());
        Assertions.assertEquals("john", users.get(0).getUserFirstName());
    }

    @Test
    void invalidDateValueThrows() {
        Assertions.assertThrows(SearchQueryException.class, () -> {
            Specification<Users> spec = new SpecificationsBuilder<Users>(caseSensitive())
                    .withSearch("createdAt:not-a-date").build();
            userRepository.findAll(spec);
        });
    }

    @Test
    void canSearchLongGreaterThanEquals() {
        userRepository.save(Users.builder().userFirstName("john").userChildrenNumber(10).build());
        userRepository.save(Users.builder().userFirstName("jane").userChildrenNumber(20).build());
        Specification<Users> specification = new SpecificationsBuilder<Users>(caseSensitive())
                .withSearch("userChildrenNumber>:10").build();
        List<Users> users = userRepository.findAll(specification);
        Assertions.assertEquals(2, users.size());
    }

    @Test
    void canSearchIntLessThanEquals() {
        userRepository.save(Users.builder().userFirstName("john").userChildrenNumber(10).build());
        userRepository.save(Users.builder().userFirstName("jane").userChildrenNumber(20).build());
        Specification<Users> specification = new SpecificationsBuilder<Users>(caseSensitive())
                .withSearch("userChildrenNumber<:10").build();
        List<Users> users = userRepository.findAll(specification);
        Assertions.assertEquals(1, users.size());
        Assertions.assertEquals("john", users.get(0).getUserFirstName());
    }

    @Test
    void orOperation() {
        userRepository.save(Users.builder().userFirstName("john").build());
        userRepository.save(Users.builder().userFirstName("jane").build());
        userRepository.save(Users.builder().userFirstName("bob").build());
        Specification<Users> specification = new SpecificationsBuilder<Users>(caseSensitive())
                .withSearch("userFirstName:john OR userFirstName:jane").build();
        List<Users> users = userRepository.findAll(specification);
        Assertions.assertEquals(2, users.size());
    }

    @Test
    void canSearchWithNegativeNumber() {
        userRepository.save(Users.builder().userFirstName("john").userChildrenNumber(-5).build());
        userRepository.save(Users.builder().userFirstName("jane").userChildrenNumber(10).build());
        Specification<Users> specification = new SpecificationsBuilder<Users>(caseSensitive())
                .withSearch("userChildrenNumber>-1").build();
        List<Users> users = userRepository.findAll(specification);
        Assertions.assertEquals(1, users.size());
        Assertions.assertEquals("jane", users.get(0).getUserFirstName());
    }

    @Test
    void caseInsensitiveExactEquals() {
        userRepository.save(Users.builder().userFirstName("John").build());
        userRepository.save(Users.builder().userFirstName("JOHN").build());
        userRepository.save(Users.builder().userFirstName("jane").build());
        Specification<Users> specification = new SpecificationsBuilder<Users>(caseInsensitive())
                .withSearch("userFirstName:john").build();
        List<Users> users = userRepository.findAll(specification);
        Assertions.assertEquals(2, users.size());
    }

    @Test
    void caseInsensitiveNotEquals() {
        userRepository.save(Users.builder().userFirstName("John").build());
        userRepository.save(Users.builder().userFirstName("JOHN").build());
        userRepository.save(Users.builder().userFirstName("jane").build());
        Specification<Users> specification = new SpecificationsBuilder<Users>(caseInsensitive())
                .withSearch("userFirstName!john").build();
        List<Users> users = userRepository.findAll(specification);
        Assertions.assertEquals(1, users.size());
        Assertions.assertEquals("jane", users.get(0).getUserFirstName());
    }

    @Test
    void canSearchShortField() {
        userRepository.save(Users.builder().userFirstName("john").userLevel((short) 3).build());
        userRepository.save(Users.builder().userFirstName("jane").userLevel((short) 7).build());
        Specification<Users> specification = new SpecificationsBuilder<Users>(caseSensitive())
                .withSearch("userLevel>5").build();
        List<Users> users = userRepository.findAll(specification);
        Assertions.assertEquals(1, users.size());
        Assertions.assertEquals("jane", users.get(0).getUserFirstName());
    }

    @Test
    void canSearchShortFieldEquals() {
        userRepository.save(Users.builder().userFirstName("john").userLevel((short) 3).build());
        userRepository.save(Users.builder().userFirstName("jane").userLevel((short) 7).build());
        Specification<Users> specification = new SpecificationsBuilder<Users>(caseSensitive())
                .withSearch("userLevel:3").build();
        List<Users> users = userRepository.findAll(specification);
        Assertions.assertEquals(1, users.size());
        Assertions.assertEquals("john", users.get(0).getUserFirstName());
    }

    @Test
    void invalidLocalDateValueThrows() {
        Assertions.assertThrows(SearchQueryException.class, () -> {
            Specification<Users> spec = new SpecificationsBuilder<Users>(caseSensitive())
                    .withSearch("updatedDateAt:not-a-date").build();
            userRepository.findAll(spec);
        });
    }

    @Test
    void invalidIntegerValueThrows() {
        Assertions.assertThrows(SearchQueryException.class, () -> {
            Specification<Users> spec = new SpecificationsBuilder<Users>(caseSensitive())
                    .withSearch("userChildrenNumber:abc").build();
            userRepository.findAll(spec);
        });
    }

    @Test
    void invalidUuidValueThrows() {
        Assertions.assertThrows(SearchQueryException.class, () -> {
            Specification<Users> spec = new SpecificationsBuilder<Users>(caseSensitive())
                    .withSearch("uuid:not-a-uuid").build();
            userRepository.findAll(spec);
        });
    }

    @Test
    void escapedFormFeedInStringValue() {
        userRepository.save(Users.builder().userFirstName("form\ffeed").build());
        userRepository.save(Users.builder().userFirstName("normal").build());
        Specification<Users> specification = new SpecificationsBuilder<Users>(caseSensitive())
                .withSearch("userFirstName:'form\\ffeed'").build();
        List<Users> users = userRepository.findAll(specification);
        Assertions.assertEquals(1, users.size());
    }

    @Test
    void escapedBackspaceInStringValue() {
        userRepository.save(Users.builder().userFirstName("back\bspace").build());
        userRepository.save(Users.builder().userFirstName("normal").build());
        Specification<Users> specification = new SpecificationsBuilder<Users>(caseSensitive())
                .withSearch("userFirstName:'back\\bspace'").build();
        List<Users> users = userRepository.findAll(specification);
        Assertions.assertEquals(1, users.size());
    }

    @Test
    void defaultLogicalOpIsTreatedAsAnd() {
        userRepository.save(Users.builder().userFirstName("john").userChildrenNumber(5).build());
        userRepository.save(Users.builder().userFirstName("jane").userChildrenNumber(3).build());
        Specification<Users> specification = new SpecificationsBuilder<Users>(caseSensitive())
                .withSearch("userFirstName:john AND userChildrenNumber:5").build();
        List<Users> users = userRepository.findAll(specification);
        Assertions.assertEquals(1, users.size());
        Assertions.assertEquals("john", users.get(0).getUserFirstName());
    }

    @Test
    void doesntStartWithOperation() {
        userRepository.save(Users.builder().userFirstName("john").build());
        userRepository.save(Users.builder().userFirstName("jane").build());
        userRepository.save(Users.builder().userFirstName("bob").build());
        Specification<Users> specification = new SpecificationsBuilder<Users>(caseSensitive())
                .withSearch("userFirstName!j*").build();
        List<Users> users = userRepository.findAll(specification);
        Assertions.assertEquals(1, users.size());
        Assertions.assertEquals("bob", users.get(0).getUserFirstName());
    }

    @Test
    void doesntEndWithOperation() {
        userRepository.save(Users.builder().userFirstName("john").build());
        userRepository.save(Users.builder().userFirstName("jane").build());
        userRepository.save(Users.builder().userFirstName("bob").build());
        Specification<Users> specification = new SpecificationsBuilder<Users>(caseSensitive())
                .withSearch("userFirstName!*n").build();
        List<Users> users = userRepository.findAll(specification);
        Assertions.assertEquals(2, users.size());
    }

    @Test
    void doesntContainOperation() {
        userRepository.save(Users.builder().userFirstName("john").build());
        userRepository.save(Users.builder().userFirstName("jane").build());
        userRepository.save(Users.builder().userFirstName("bob").build());
        Specification<Users> specification = new SpecificationsBuilder<Users>(caseSensitive())
                .withSearch("userFirstName!*an*").build();
        List<Users> users = userRepository.findAll(specification);
        Assertions.assertEquals(2, users.size());
    }

    @Test
    void shortFieldLessThanEquals() {
        userRepository.save(Users.builder().userFirstName("john").userLevel((short) 3).build());
        userRepository.save(Users.builder().userFirstName("jane").userLevel((short) 7).build());
        Specification<Users> specification = new SpecificationsBuilder<Users>(caseSensitive())
                .withSearch("userLevel<:5").build();
        List<Users> users = userRepository.findAll(specification);
        Assertions.assertEquals(1, users.size());
        Assertions.assertEquals("john", users.get(0).getUserFirstName());
    }

    @Test
    void shortFieldGreaterThanEquals() {
        userRepository.save(Users.builder().userFirstName("john").userLevel((short) 3).build());
        userRepository.save(Users.builder().userFirstName("jane").userLevel((short) 7).build());
        Specification<Users> specification = new SpecificationsBuilder<Users>(caseSensitive())
                .withSearch("userLevel>:5").build();
        List<Users> users = userRepository.findAll(specification);
        Assertions.assertEquals(1, users.size());
        Assertions.assertEquals("jane", users.get(0).getUserFirstName());
    }

    @Test
    void collectionFieldEqualsThrows() {
        Author author = new Author();
        author.setName("author1");
        authorRepository.save(author);
        Specification<Author> spec = new SpecificationsBuilder<Author>(caseSensitive())
                .withSearch("books:something").build();
        Assertions.assertThrows(SearchQueryException.class, () -> authorRepository.findAll(spec));
    }

    @Test
    void collectionFieldIsNotNull() {
        Author john = new Author();
        john.setName("john");
        john.addBook(new Book());
        authorRepository.save(john);
        Author jane = new Author();
        jane.setName("jane");
        authorRepository.save(jane);
        Specification<Author> spec = new SpecificationsBuilder<Author>(caseSensitive())
                .withSearch("books IS NOT NULL").build();
        Assertions.assertThrows(UnsupportedOperationException.class, () -> authorRepository.findAll(spec));
    }

    @Test
    void escapedTabInStringSearch() {
        userRepository.save(Users.builder().userFirstName("col1\tcol2").build());
        userRepository.save(Users.builder().userFirstName("other").build());
        Specification<Users> spec = new SpecificationsBuilder<Users>(caseSensitive())
                .withSearch("userFirstName:'col1\\tcol2'").build();
        List<Users> users = userRepository.findAll(spec);
        Assertions.assertEquals(1, users.size());
    }

    @Test
    void booleanIsNull() {
        Users u1 = Users.builder().userFirstName("hasAdmin").isAdmin(true).build();
        Users u2 = Users.builder().userFirstName("noAdmin").build();
        u2.setIsAdmin(null);
        userRepository.save(u1);
        userRepository.save(u2);
        Specification<Users> spec = new SpecificationsBuilder<Users>(caseSensitive())
                .withSearch("isAdmin IS NULL").build();
        List<Users> users = userRepository.findAll(spec);
        Assertions.assertEquals(1, users.size());
        Assertions.assertEquals("noAdmin", users.get(0).getUserFirstName());
    }

    @Test
    void booleanIsNotNull() {
        Users u1 = Users.builder().userFirstName("hasAdmin").isAdmin(true).build();
        Users u2 = Users.builder().userFirstName("noAdmin").build();
        u2.setIsAdmin(null);
        userRepository.save(u1);
        userRepository.save(u2);
        Specification<Users> spec = new SpecificationsBuilder<Users>(caseSensitive())
                .withSearch("isAdmin IS NOT NULL").build();
        List<Users> users = userRepository.findAll(spec);
        Assertions.assertEquals(1, users.size());
        Assertions.assertEquals("hasAdmin", users.get(0).getUserFirstName());
    }

    @Test
    void integerInArray() {
        userRepository.save(Users.builder().userFirstName("john").userChildrenNumber(2).build());
        userRepository.save(Users.builder().userFirstName("jane").userChildrenNumber(5).build());
        userRepository.save(Users.builder().userFirstName("joe").userChildrenNumber(10).build());
        Specification<Users> spec = new SpecificationsBuilder<Users>(caseSensitive())
                .withSearch("userChildrenNumber IN [2,10]").build();
        List<Users> users = userRepository.findAll(spec);
        Assertions.assertEquals(2, users.size());
    }

    @Test
    void integerNotInArray() {
        userRepository.save(Users.builder().userFirstName("john").userChildrenNumber(2).build());
        userRepository.save(Users.builder().userFirstName("jane").userChildrenNumber(5).build());
        userRepository.save(Users.builder().userFirstName("joe").userChildrenNumber(10).build());
        Specification<Users> spec = new SpecificationsBuilder<Users>(caseSensitive())
                .withSearch("userChildrenNumber NOT IN [2,10]").build();
        List<Users> users = userRepository.findAll(spec);
        Assertions.assertEquals(1, users.size());
        Assertions.assertEquals("jane", users.get(0).getUserFirstName());
    }

    @Test
    void caseInsensitiveGreaterThan() {
        userRepository.save(Users.builder().userFirstName("Abel").build());
        userRepository.save(Users.builder().userFirstName("Bob").build());
        userRepository.save(Users.builder().userFirstName("Charlie").build());
        Specification<Users> spec = new SpecificationsBuilder<Users>(caseInsensitive())
                .withSearch("userFirstName>bob").build();
        List<Users> users = userRepository.findAll(spec);
        Assertions.assertEquals(1, users.size());
        Assertions.assertEquals("Charlie", users.get(0).getUserFirstName());
    }

    @Test
    void caseInsensitiveGreaterThanEquals() {
        userRepository.save(Users.builder().userFirstName("Abel").build());
        userRepository.save(Users.builder().userFirstName("Bob").build());
        userRepository.save(Users.builder().userFirstName("Charlie").build());
        Specification<Users> spec = new SpecificationsBuilder<Users>(caseInsensitive())
                .withSearch("userFirstName>:bob").build();
        List<Users> users = userRepository.findAll(spec);
        Assertions.assertEquals(2, users.size());
    }

    @Test
    void enumInArray() {
        userRepository.save(Users.builder().userFirstName("john").type(UserType.TEAM_MEMBER).build());
        userRepository.save(Users.builder().userFirstName("jane").type(UserType.ADMINISTRATOR).build());
        userRepository.save(Users.builder().userFirstName("joe").type(UserType.TEAM_MEMBER).build());
        Specification<Users> spec = new SpecificationsBuilder<Users>(caseSensitive())
                .withSearch("type IN [ADMINISTRATOR]").build();
        List<Users> users = userRepository.findAll(spec);
        Assertions.assertEquals(1, users.size());
        Assertions.assertEquals("jane", users.get(0).getUserFirstName());
    }

    @Test
    void enumNotInArray() {
        userRepository.save(Users.builder().userFirstName("john").type(UserType.TEAM_MEMBER).build());
        userRepository.save(Users.builder().userFirstName("jane").type(UserType.ADMINISTRATOR).build());
        Specification<Users> spec = new SpecificationsBuilder<Users>(caseSensitive())
                .withSearch("type NOT IN [ADMINISTRATOR]").build();
        List<Users> users = userRepository.findAll(spec);
        Assertions.assertEquals(1, users.size());
        Assertions.assertEquals("john", users.get(0).getUserFirstName());
    }

    @Test
    void uuidInArray() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        UUID id3 = UUID.randomUUID();
        userRepository.save(Users.builder().userFirstName("john").uuid(id1).build());
        userRepository.save(Users.builder().userFirstName("jane").uuid(id2).build());
        userRepository.save(Users.builder().userFirstName("joe").uuid(id3).build());
        Specification<Users> spec = new SpecificationsBuilder<Users>(caseSensitive())
                .withSearch("uuid IN [" + id1 + "," + id3 + "]").build();
        List<Users> users = userRepository.findAll(spec);
        Assertions.assertEquals(2, users.size());
    }

    @Test
    void dateNotEquals() throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        Date d1 = sdf.parse("2023-01-15");
        Date d2 = sdf.parse("2023-06-20");
        userRepository.save(Users.builder().userFirstName("john").createdAt(d1).build());
        userRepository.save(Users.builder().userFirstName("jane").createdAt(d2).build());
        Specification<Users> spec = new SpecificationsBuilder<Users>(caseSensitive())
                .withSearch("createdAt!2023-01-15").build();
        List<Users> users = userRepository.findAll(spec);
        Assertions.assertEquals(1, users.size());
        Assertions.assertEquals("jane", users.get(0).getUserFirstName());
    }

    @Test
    void dateInArray() throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        Date d1 = sdf.parse("2023-01-15");
        Date d2 = sdf.parse("2023-06-20");
        Date d3 = sdf.parse("2023-12-31");
        userRepository.save(Users.builder().userFirstName("john").createdAt(d1).build());
        userRepository.save(Users.builder().userFirstName("jane").createdAt(d2).build());
        userRepository.save(Users.builder().userFirstName("joe").createdAt(d3).build());
        Specification<Users> spec = new SpecificationsBuilder<Users>(caseSensitive())
                .withSearch("createdAt IN [2023-01-15,2023-12-31]").build();
        List<Users> users = userRepository.findAll(spec);
        Assertions.assertEquals(2, users.size());
    }

    @Test
    void invalidDateValueThrowsInvalidValue() {
        Assertions.assertThrows(SearchQueryException.class, () -> {
            Specification<Users> spec = new SpecificationsBuilder<Users>(caseSensitive())
                    .withSearch("createdAt:not-a-date").build();
            userRepository.findAll(spec);
        });
    }

    @Test
    void invalidLocalDateTimeValueThrows() {
        Assertions.assertThrows(SearchQueryException.class, () -> {
            Specification<Users> spec = new SpecificationsBuilder<Users>(caseSensitive())
                    .withSearch("updatedAt:not-a-datetime").build();
            userRepository.findAll(spec);
        });
    }

    @Test
    void shortFieldEquals() {
        userRepository.save(Users.builder().userFirstName("john").userLevel((short) 5).build());
        userRepository.save(Users.builder().userFirstName("jane").userLevel((short) 10).build());
        Specification<Users> spec = new SpecificationsBuilder<Users>(caseSensitive())
                .withSearch("userLevel:5").build();
        List<Users> users = userRepository.findAll(spec);
        Assertions.assertEquals(1, users.size());
        Assertions.assertEquals("john", users.get(0).getUserFirstName());
    }

    @Test
    void shortFieldNotEquals() {
        userRepository.save(Users.builder().userFirstName("john").userLevel((short) 5).build());
        userRepository.save(Users.builder().userFirstName("jane").userLevel((short) 10).build());
        Specification<Users> spec = new SpecificationsBuilder<Users>(caseSensitive())
                .withSearch("userLevel!5").build();
        List<Users> users = userRepository.findAll(spec);
        Assertions.assertEquals(1, users.size());
        Assertions.assertEquals("jane", users.get(0).getUserFirstName());
    }

    @Test
    void shortFieldInArray() {
        userRepository.save(Users.builder().userFirstName("john").userLevel((short) 3).build());
        userRepository.save(Users.builder().userFirstName("jane").userLevel((short) 5).build());
        userRepository.save(Users.builder().userFirstName("joe").userLevel((short) 10).build());
        Specification<Users> spec = new SpecificationsBuilder<Users>(caseSensitive())
                .withSearch("userLevel IN [3,10]").build();
        List<Users> users = userRepository.findAll(spec);
        Assertions.assertEquals(2, users.size());
    }

    @Test
    void shortFieldIsNull() {
        Users u1 = Users.builder().userFirstName("john").userLevel((short) 5).build();
        Users u2 = Users.builder().userFirstName("jane").build();
        u2.setUserLevel(null);
        userRepository.save(u1);
        userRepository.save(u2);
        Specification<Users> spec = new SpecificationsBuilder<Users>(caseSensitive())
                .withSearch("userLevel IS NULL").build();
        List<Users> users = userRepository.findAll(spec);
        Assertions.assertEquals(1, users.size());
        Assertions.assertEquals("jane", users.get(0).getUserFirstName());
    }

    @Test
    void shortFieldBetween() {
        userRepository.save(Users.builder().userFirstName("john").userLevel((short) 3).build());
        userRepository.save(Users.builder().userFirstName("jane").userLevel((short) 7).build());
        userRepository.save(Users.builder().userFirstName("joe").userLevel((short) 15).build());
        Specification<Users> spec = new SpecificationsBuilder<Users>(caseSensitive())
                .withSearch("userLevel BETWEEN 5 AND 10").build();
        List<Users> users = userRepository.findAll(spec);
        Assertions.assertEquals(1, users.size());
        Assertions.assertEquals("jane", users.get(0).getUserFirstName());
    }

    @Test
    void durationInArray() {
        Duration d1 = Duration.ofDays(10);
        Duration d2 = Duration.ofDays(30);
        Duration d3 = Duration.ofDays(60);
        userRepository.save(Users.builder().userFirstName("john").validityDuration(d1).build());
        userRepository.save(Users.builder().userFirstName("jane").validityDuration(d2).build());
        userRepository.save(Users.builder().userFirstName("joe").validityDuration(d3).build());
        Specification<Users> spec = new SpecificationsBuilder<Users>(caseSensitive())
                .withSearch("validityDuration IN [PT240H,PT1440H]").build();
        List<Users> users = userRepository.findAll(spec);
        Assertions.assertEquals(2, users.size());
    }

    @Test
    void durationLessThanEquals() {
        userRepository.save(Users.builder().userFirstName("john").validityDuration(Duration.ofDays(10)).build());
        userRepository.save(Users.builder().userFirstName("jane").validityDuration(Duration.ofDays(30)).build());
        userRepository.save(Users.builder().userFirstName("joe").validityDuration(Duration.ofDays(60)).build());
        Specification<Users> spec = new SpecificationsBuilder<Users>(caseSensitive())
                .withSearch("validityDuration<:PT720H").build();
        List<Users> users = userRepository.findAll(spec);
        Assertions.assertEquals(2, users.size());
    }

    @Test
    void durationGreaterThanEquals() {
        userRepository.save(Users.builder().userFirstName("john").validityDuration(Duration.ofDays(10)).build());
        userRepository.save(Users.builder().userFirstName("jane").validityDuration(Duration.ofDays(30)).build());
        userRepository.save(Users.builder().userFirstName("joe").validityDuration(Duration.ofDays(60)).build());
        Specification<Users> spec = new SpecificationsBuilder<Users>(caseSensitive())
                .withSearch("validityDuration>:PT720H").build();
        List<Users> users = userRepository.findAll(spec);
        Assertions.assertEquals(2, users.size());
    }

    @Test
    void whiteListWithMultipleFieldsAllowsListed() {
        SearchSpec whiteListSpec = createAnnotation(true, new String[]{"userFirstName", "userLastName"}, new String[]{});
        userRepository.save(Users.builder().userFirstName("john").userLastName("doe").build());
        userRepository.save(Users.builder().userFirstName("jane").userLastName("smith").build());
        Specification<Users> spec = new SpecificationsBuilder<Users>(whiteListSpec)
                .withSearch("userFirstName:john AND userLastName:doe").build();
        List<Users> users = userRepository.findAll(spec);
        Assertions.assertEquals(1, users.size());
        Assertions.assertEquals("john", users.get(0).getUserFirstName());
    }

    @Test
    void floatInArray() {
        userRepository.save(Users.builder().userFirstName("john").userSalary(1000.0F).build());
        userRepository.save(Users.builder().userFirstName("jane").userSalary(2000.0F).build());
        userRepository.save(Users.builder().userFirstName("joe").userSalary(3000.0F).build());
        Specification<Users> spec = new SpecificationsBuilder<Users>(caseSensitive())
                .withSearch("userSalary IN [1000.0,3000.0]").build();
        List<Users> users = userRepository.findAll(spec);
        Assertions.assertEquals(2, users.size());
    }

    @Test
    void doubleInArray() {
        userRepository.save(Users.builder().userFirstName("john").userAgeInSeconds(100.0).build());
        userRepository.save(Users.builder().userFirstName("jane").userAgeInSeconds(200.0).build());
        userRepository.save(Users.builder().userFirstName("joe").userAgeInSeconds(300.0).build());
        Specification<Users> spec = new SpecificationsBuilder<Users>(caseSensitive())
                .withSearch("userAgeInSeconds IN [100.0,300.0]").build();
        List<Users> users = userRepository.findAll(spec);
        Assertions.assertEquals(2, users.size());
    }

    @Test
    void localDateInArray() {
        LocalDate d1 = LocalDate.of(2023, 1, 15);
        LocalDate d2 = LocalDate.of(2023, 6, 20);
        LocalDate d3 = LocalDate.of(2023, 12, 31);
        userRepository.save(Users.builder().userFirstName("john").updatedDateAt(d1).build());
        userRepository.save(Users.builder().userFirstName("jane").updatedDateAt(d2).build());
        userRepository.save(Users.builder().userFirstName("joe").updatedDateAt(d3).build());
        Specification<Users> spec = new SpecificationsBuilder<Users>(caseSensitive())
                .withSearch("updatedDateAt IN [2023-01-15,2023-12-31]").build();
        List<Users> users = userRepository.findAll(spec);
        Assertions.assertEquals(2, users.size());
    }

    @Test
    void instantBetween() {
        Instant i1 = Instant.parse("2023-01-15T00:00:00Z");
        Instant i2 = Instant.parse("2023-06-20T00:00:00Z");
        Instant i3 = Instant.parse("2023-12-31T00:00:00Z");
        userRepository.save(Users.builder().userFirstName("john").updatedInstantAt(i1).build());
        userRepository.save(Users.builder().userFirstName("jane").updatedInstantAt(i2).build());
        userRepository.save(Users.builder().userFirstName("joe").updatedInstantAt(i3).build());
        Specification<Users> spec = new SpecificationsBuilder<Users>(caseSensitive())
                .withSearch("updatedInstantAt BETWEEN '2023-01-01T00:00:00Z' AND '2023-07-01T00:00:00Z'").build();
        List<Users> users = userRepository.findAll(spec);
        Assertions.assertEquals(2, users.size());
    }

    @Test
    void booleanInArray() {
        userRepository.save(Users.builder().userFirstName("admin").isAdmin(true).build());
        userRepository.save(Users.builder().userFirstName("user").isAdmin(false).build());
        Specification<Users> spec = new SpecificationsBuilder<Users>(caseSensitive())
                .withSearch("isAdmin IN [true]").build();
        List<Users> users = userRepository.findAll(spec);
        Assertions.assertEquals(1, users.size());
        Assertions.assertEquals("admin", users.get(0).getUserFirstName());
    }

    @Test
    void uuidNotInArray() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        UUID id3 = UUID.randomUUID();
        userRepository.save(Users.builder().userFirstName("john").uuid(id1).build());
        userRepository.save(Users.builder().userFirstName("jane").uuid(id2).build());
        userRepository.save(Users.builder().userFirstName("joe").uuid(id3).build());
        Specification<Users> spec = new SpecificationsBuilder<Users>(caseSensitive())
                .withSearch("uuid NOT IN [" + id1 + "]").build();
        List<Users> users = userRepository.findAll(spec);
        Assertions.assertEquals(2, users.size());
    }

    @Test
    void stringDoesntContainCaseSensitive() {
        userRepository.save(Users.builder().userFirstName("John").build());
        userRepository.save(Users.builder().userFirstName("Jane").build());
        userRepository.save(Users.builder().userFirstName("Bob").build());
        Specification<Users> spec = new SpecificationsBuilder<Users>(caseSensitive())
                .withSearch("userFirstName!*an*").build();
        List<Users> users = userRepository.findAll(spec);
        Assertions.assertEquals(2, users.size());
    }

    @Test
    void stringDoesntStartWithCaseSensitive() {
        userRepository.save(Users.builder().userFirstName("John").build());
        userRepository.save(Users.builder().userFirstName("Jane").build());
        userRepository.save(Users.builder().userFirstName("Bob").build());
        Specification<Users> spec = new SpecificationsBuilder<Users>(caseSensitive())
                .withSearch("userFirstName!J*").build();
        List<Users> users = userRepository.findAll(spec);
        Assertions.assertEquals(1, users.size());
        Assertions.assertEquals("Bob", users.get(0).getUserFirstName());
    }

    @Test
    void stringDoesntEndWithCaseSensitive() {
        userRepository.save(Users.builder().userFirstName("John").build());
        userRepository.save(Users.builder().userFirstName("Jane").build());
        userRepository.save(Users.builder().userFirstName("Bob").build());
        Specification<Users> spec = new SpecificationsBuilder<Users>(caseSensitive())
                .withSearch("userFirstName!*ne").build();
        List<Users> users = userRepository.findAll(spec);
        Assertions.assertEquals(2, users.size());
    }
}
