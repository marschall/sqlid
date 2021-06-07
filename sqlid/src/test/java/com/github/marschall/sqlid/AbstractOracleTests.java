package com.github.marschall.sqlid;

import static org.springframework.test.context.TestConstructor.AutowireMode.ALL;

import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * Abstract base class for all Oracle based tests to make sure Spring Context caching is used.
 */
@SpringJUnitConfig
@ContextConfiguration(classes = OracleConfiguration.class)
@TestConstructor(autowireMode = ALL)
abstract class AbstractOracleTests {

}
