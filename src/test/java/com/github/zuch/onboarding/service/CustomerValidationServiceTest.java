package com.github.zuch.onboarding.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.zuch.onboarding.mapper.CustomerMapper;
import com.github.zuch.onboarding.model.Validation;
import com.github.zuch.onboarding.model.config.AppConfigProperties;
import com.github.zuch.onboarding.model.request.RegistrationRequest;
import com.github.zuch.onboarding.persistence.AccountRepository;
import com.github.zuch.onboarding.persistence.RoleRepository;
import com.github.zuch.onboarding.persistence.UserRepository;
import com.github.zuch.onboarding.persistence.entity.Account;
import com.github.zuch.onboarding.persistence.entity.Role;
import com.github.zuch.onboarding.persistence.entity.Roles;
import com.github.zuch.onboarding.persistence.entity.User;
import com.github.zuch.onboarding.validation.CustomerValidationService;
import com.github.zuch.onboarding.validation.ValidationMessageUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ResourceLoader;
import org.springframework.test.context.ActiveProfiles;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("h2")
class CustomerValidationServiceTest {

    @Autowired
    private CustomerMapper customerMapper;
    @Autowired
    private ResourceLoader resourceLoader;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private AppConfigProperties appConfigProperties;
    @Autowired
    private ValidationMessageUtil validationMessageUtil;
    @Autowired
    private CustomerValidationService customerValidationService;

    @BeforeEach
    public void setup() {
        userRepository.deleteAll();
        roleRepository.deleteAll();
    }

    @Test
    void given_validRegistration_when_ValidationChecked_then_ValidValidationReturned() throws IOException {
        // given
        File file = resourceLoader.getResource("classpath:json/registration_valid.json").getFile();
        RegistrationRequest registrationRequest = objectMapper.readValue(file, RegistrationRequest.class);

        // when
        Validation validation = customerValidationService.validateReg(registrationRequest);

        // then
        assertTrue(validation.isValid());
    }

    @Test
    void given_invalidRegistrationNoAddressAndUsername_when_ValidationChecked_then_InValidValidationReturned() throws IOException {
        // given
        File file = resourceLoader.getResource("classpath:json/registration_invalid_no_address_and_username.json").getFile();
        RegistrationRequest registrationRequest = objectMapper.readValue(file, RegistrationRequest.class);

        // when
        Validation validation = customerValidationService.validateReg(registrationRequest);

        // then
        assertFalse(validation.isValid());

        Set<String> messages = validation.getValidationMessages();
        String[] messagesArray = new String[messages.size()];
        messages.toArray(messagesArray);

        assertEquals("$.address: is missing but it is required", messagesArray[0]);
        assertEquals("$.username: is missing but it is required", messagesArray[1]);
    }

    @Test
    void given_invalidRegistrationEmptyCountryCode_when_ValidationChecked_then_InValidValidationReturned() throws IOException {
        // given
        File file = resourceLoader.getResource("classpath:json/registration_invalid_empty_countrycode.json").getFile();
        RegistrationRequest registrationRequest = objectMapper.readValue(file, RegistrationRequest.class);

        // when
        Validation validation = customerValidationService.validateReg(registrationRequest);

        // then
        assertFalse(validation.isValid());
        assertEquals("$.idDocument.countryCode: is missing but it is required", validation.getValidationMessages().stream().findFirst().get());
    }

    @Test
    void given_invalidRegistrationIncorrectCountryCode_when_ValidationChecked_then_InValidValidationReturned() throws IOException {
        // given
        File file = resourceLoader.getResource("classpath:json/registration_invalid_incorrect_countrycode.json").getFile();
        RegistrationRequest registrationRequest = objectMapper.readValue(file, RegistrationRequest.class);

        // when
        Validation validation = customerValidationService.validateReg(registrationRequest);

        // then
        assertFalse(validation.isValid());
        assertEquals("CountyCode [FR] is not valid or one of the allowed countries", validation.getValidationMessages().stream().findFirst().get());
    }

    @Test
    void given_invalidRegistrationYoungerThan18_when_ValidationChecked_then_InValidValidationReturned() throws IOException {
        // given
        File file = resourceLoader.getResource("classpath:json/registration_valid.json").getFile();
        RegistrationRequest registrationRequest = objectMapper.readValue(file, RegistrationRequest.class);
        registrationRequest.setDateOfBirth(LocalDate.now().minusYears(17));

        // when
        Validation validation = customerValidationService.validateReg(registrationRequest);

        // then
        assertFalse(validation.isValid());
        assertEquals("Customer must be older than 18 years old to register for an account", validation.getValidationMessages().stream().findFirst().get());
    }

    @Test
    void given_invalidRegistrationUsernameAlreadyExists_when_ValidationChecked_then_InValidValidationReturned() throws IOException {
        // given
        File file = resourceLoader.getResource("classpath:json/registration_valid.json").getFile();
        RegistrationRequest reg = objectMapper.readValue(file, RegistrationRequest.class);

        // persist role
        Role role = Role.builder().username(reg.getUsername()).name(Roles.CUSTOMER).build();
        Role savedRolde = roleRepository.save(role);

        // persist the same customer already
        Account account = Account.builder().username(reg.getUsername()).build();
        User user = customerMapper.mapToUser(reg, savedRolde, account, "w2kfHZriif");
        userRepository.save(user);

        // when
        Validation validation = customerValidationService.validateReg(reg);

        // then
        assertFalse(validation.isValid());
        assertEquals("username [theone] already exists", validation.getValidationMessages().stream().findFirst().get());
    }
}
