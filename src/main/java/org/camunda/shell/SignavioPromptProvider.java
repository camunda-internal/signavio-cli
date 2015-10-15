package org.camunda.shell;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.shell.plugin.support.DefaultPromptProvider;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SignavioPromptProvider extends DefaultPromptProvider {

  @Inject
  private ShellConfiguration shellConfiguration;

  @Override
  public String getPrompt() {
    if (shellConfiguration.getEndpoint() == null) {
      return "signavio>";
    } else {
      return "signavio@" + shellConfiguration.getEndpoint() + ">";
    }
  }

  @Override
  public String getProviderName() {
    return "Signavio Prompt Provider";
  }
}
