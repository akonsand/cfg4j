/*
 * Copyright 2015 Norbert Potocki (norbert.potocki@nort.pl)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package pl.nort.config.source;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import pl.nort.config.utils.FileUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Properties;

/**
 * Temporary local git repository that contains configuration files.
 */
class TempConfigurationGitRepo {

  private final Git repo;

  /**
   * Create temporary, local git repository. When you're done using it remove it by invoking {@link #remove()}
   * method.
   *
   * @throws IOException     when unable to create local directories
   * @throws GitAPIException when unable to execute git operations
   */
  public TempConfigurationGitRepo() throws IOException, GitAPIException {
    File tempFile = File.createTempFile("test-repo.git", "");
    repo = createLocalRepo(tempFile);
  }

  /**
   * Get absolute path to this repository.
   *
   * @return path to the repository
   */
  public String getURI() {
    return repo.getRepository().getWorkTree().getAbsolutePath();
  }

  /**
   * Change active branch to {@code branch}. Create the branch if it doesn't exist.
   *
   * @param branch branch to activate
   * @throws GitAPIException when unable to change branch.
   */
  public void changeBranchTo(String branch) throws GitAPIException {
    repo
        .checkout()
        .setCreateBranch(true)
        .setName(branch)
        .call();
  }

  /**
   * Change the {@code key} property to {@code value} and store it in a {@code propFilePath} properties file. Commit
   * the change.
   *
   * @param propFilePath relative path to the properties file in this repository
   * @param key          property key
   * @param value        property value
   * @throws IOException     when unable to modify properties file
   * @throws GitAPIException when unable to commit changes
   */
  public void changeProperty(String propFilePath, String key, String value) throws IOException, GitAPIException {
    writePropertyToFile(propFilePath, key, value);
    commitChangesTo(repo);
  }

  /**
   * Delete file from this repository.
   *
   * @param filePath relative file path to delete
   */
  public void deleteFile(String filePath) {
    new File(getURI() + "/" + filePath).delete();
  }

  /**
   * Remove this repository.
   *
   * @throws IOException when unable to remove directory
   */
  public void remove() throws IOException {
    repo.close();
    FileUtils.deleteDir(new File(getURI()));
  }

  private Git createLocalRepo(File path) throws IOException, GitAPIException {
    path.delete();
    return Git.init()
        .setDirectory(path)
        .call();
  }

  private void writePropertyToFile(String propFilePath, String key, String value) throws IOException {
    OutputStream out = new FileOutputStream(getURI() + "/" + propFilePath);
    Properties properties = new Properties();
    properties.put(key, value);
    properties.store(out, "");
    out.close();
  }

  private void commitChangesTo(Git repo) throws GitAPIException {
    repo.add()
        .addFilepattern(".")
        .call();

    repo.commit()
        .setMessage("config change")
        .call();
  }
}
