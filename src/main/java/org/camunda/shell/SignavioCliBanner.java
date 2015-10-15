package org.camunda.shell;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.shell.plugin.support.DefaultBannerProvider;
import org.springframework.shell.support.util.FileUtils;
import org.springframework.shell.support.util.OsUtils;
import org.springframework.stereotype.Component;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SignavioCliBanner extends DefaultBannerProvider {

  @Override
  public String getBanner() {
    StringBuilder sb = new StringBuilder();
    sb.append(FileUtils.readBanner(SignavioCliBanner.class, "banner.txt"));
    sb.append(getVersion()).append(OsUtils.LINE_SEPARATOR);
    sb.append(OsUtils.LINE_SEPARATOR);

    return sb.toString();
  }

  @Override
  public String getVersion() {
    String version = null;
    Package pkg = SignavioCliBanner.class.getPackage();
    if (pkg != null) {
      version = pkg.getImplementationVersion();
    }
    return (version != null ? "v"+version : "Unknown Version");
  }

  @Override
  public String getWelcomeMessage() {
    return "Welcome to Signavio CLI";
  }

  @Override
  public String getProviderName() {
    return "Signavio CLI Banner";
  }
}
