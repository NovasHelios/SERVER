package com.heilous;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test") // src/test/resources/application-test.yml 로드
class HeilousApplicationTests {

	@Test
	void contextLoads() {
	}

}