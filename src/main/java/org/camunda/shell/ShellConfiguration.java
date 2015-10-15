package org.camunda.shell;

import org.camunda.bpm.cycle.configuration.CycleConfiguration;
import org.camunda.bpm.cycle.connector.signavio.SignavioConnector;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan("org.camunda.shell")
public class ShellConfiguration {

  private String endpoint = null;

  @Bean
  public SignavioConnector signavioConnector() {
    return new SignavioConnector();
  }

  @Bean
  public CycleConfiguration cycleConfiguration() {
    return new CycleConfiguration();
  }

  public String getEndpoint() {
    return endpoint;
  }

  public void setEndpoint(String endpoint) {
    this.endpoint = endpoint;
  }

}
