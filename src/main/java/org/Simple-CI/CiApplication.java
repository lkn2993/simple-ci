package org.Simple-CI;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
/**
 * CiApplication class containing the main method.
 * Main method boots spring framework.
 */
@SpringBootApplication
public class CiApplication {
	public static CIDataBase CIDB = new CIDataBase("");;
	public static void main(String[] args) {
		SpringApplication.run(CiApplication.class, args);
	}

}
