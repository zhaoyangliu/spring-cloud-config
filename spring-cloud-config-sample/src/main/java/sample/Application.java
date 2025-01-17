/*
 * Copyright 2018-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package sample;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RefreshScope
@RestController
@SpringBootApplication
public class Application {

	@Autowired
	private Environment environment;

	@Value("${hostedcommander.telemetry.redshift.password:Hello world - Config Server is not working..pelase check}")
	private String hostedcommanderRedshiftPassword;

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

	// @RequestMapping("/")
	// public String query(@RequestParam("q") String q) {
	// return this.environment.getProperty(q);
	// }

	@RequestMapping("/getHostedcommanderRedshiftPassword")
	public String query() {
		return this.hostedcommanderRedshiftPassword;
	}

}
