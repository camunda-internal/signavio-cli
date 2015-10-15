package org.camunda.shell.commands;

import org.camunda.AbstractShellIT;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.shell.core.CommandResult;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@Ignore
public class SignavioTest extends AbstractShellIT {

  @Test
  public void login() {
    CommandResult commandResult = getShell().executeCommand("login --user xxx --password yyy --url http://zzz");
    assertNull(commandResult.getException());
    assertTrue(commandResult.isSuccess());
  }

  @Test
  public void listRoot() {
    login();
    CommandResult commandResult = getShell().executeCommand("ls");
    assertNull(commandResult.getException());
    assertTrue(commandResult.isSuccess());
  }

  @Test
  public void listPrivateFolder() {
    login();
    CommandResult commandResult = getShell().executeCommand("ls private");
    assertNull(commandResult.getException());
    assertTrue(commandResult.isSuccess());
  }

  @Ignore
  @Test
  public void exportPrivateFolder() {
    login();
    CommandResult commandResult = getShell().executeCommand("export --folder private");
    assertNull(commandResult.getException());
    assertTrue(commandResult.isSuccess());
  }

}
