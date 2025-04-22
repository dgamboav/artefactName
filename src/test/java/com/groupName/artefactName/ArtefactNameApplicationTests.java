package com.groupName.artefactName;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class ArtefactNameApplicationTests {

	@Autowired
	private ApplicationContext context;

	@Test
	void contextLoads() {
		ArtefactNameApplication.main(new String[] {});
		assertNotNull(context, "El contexto de la aplicación no se cargó correctamente.");
	}

}
