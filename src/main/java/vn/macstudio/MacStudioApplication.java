package vn.macstudio;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class MacStudioApplication {

	public static void main(String[] args) {
		ConfigurableApplicationContext context = SpringApplication.run(MacStudioApplication.class, args);
		String port = context.getEnvironment().getProperty("server.port", "5128");
		System.out.println("Local URL: http://localhost:" + port);
	}

}
