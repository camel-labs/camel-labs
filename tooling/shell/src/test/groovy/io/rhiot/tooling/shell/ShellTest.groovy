/**
 * Licensed to the Rhiot under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.rhiot.tooling.shell

import io.rhiot.utils.ssh.client.SshClient
import io.rhiot.utils.ssh.server.SshServer
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.boot.test.SpringApplicationConfiguration
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner

import java.nio.file.Paths

import static com.google.common.truth.Truth.assertThat
import static io.rhiot.utils.Networks.findAvailableTcpPort
import static io.rhiot.utils.Properties.setIntProperty
import static io.rhiot.utils.Uuids.uuid

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = Shell.class)
class ShellTest {

    // Fixtures

    static device = new SshServer().start()

    static int shellPort = findAvailableTcpPort()

    static def shellClient = new SshClient('localhost', shellPort, 'rhiot', 'rhiot')

    def file = uuid()

    def configCommand(String command, String remaining) {
        shellClient.command("${command} --host localhost --port ${device.port()} ${remaining}")
    }

    def configCommand(String command) {
        configCommand(command, '')
    }

    @BeforeClass
    static void beforeClass() {
        setIntProperty('shell.ssh.port', shellPort)
    }

    // Commands core tests

    @Test
    void shouldExecuteCommand() {
        def result = shellClient.command('shell-start')
        assertThat(result.size()).isGreaterThan(1)
        assertThat(result.first()).contains('up and running')
    }

    @Test
    void shouldPrintSingleLineOfOutput() {
        def result = shellClient.command("device-config --host localhost --port ${device.port()} /${file} foo bar")
        assertThat(result).hasSize(1)
    }

    // device-scan tests

    @Test
    void shouldPerformScan() {
        // When
        def result = shellClient.command("device-scan")

        // Then
        assertThat(result.first()).startsWith('Scanning')
    }

    // device-config tests

    @Test
    void shouldAddConfigurationFile() {
        // When
        def result = shellClient.command("device-config --host localhost --port ${device.port()} /${file} foo bar")
        def properties = new Properties()
        properties.load(new FileInputStream(new File(device.root(), file)))

        // Then
        assertThat(properties.getProperty('foo')).isEqualTo('bar')
        assertThat(result.first()).startsWith('Updated')
    }

    @Test
    void shouldAppendProperty() {
        // Given
        configCommand('device-config', "/${file} foo bar")

        // When
        configCommand('device-config', "-a /${file} foo baz")
        def properties = new Properties()
        properties.load(new FileInputStream(new File(device.root(), file)))

        // Then
        assertThat(properties.getProperty('foo')).isEqualTo('barbaz')
    }

    @Test
    void shouldHandleMissingFile() {
        def result = shellClient.command("device-config --host localhost --port ${device.port()}")
        assertThat(result.first()).contains("Parameter \'file\' is required")
    }

    // kura-config-bootdelegation tests

    @Test
    void shouldConfigureBootDelegation() {
        // When
        configCommand('kura-config-bootdelegation')

        // Then
        def properties = new Properties()
        properties.load(new FileInputStream(Paths.get(device.root().absolutePath, 'opt', 'eclipse', 'kura', 'kura', 'config.ini').toFile()))
        assertThat(properties.getProperty('org.osgi.framework.bootdelegation')).isEqualTo('sun.*,com.sun.*')
    }

    // kura-config-ini tests

    @Test
    void shouldEditKuraConfigIni() {
        // When
        configCommand('kura-config-ini', 'foo bar')

        // Then
        def properties = new Properties()
        properties.load(new FileInputStream(Paths.get(device.root().absolutePath, 'opt', 'eclipse', 'kura', 'kura', 'config.ini').toFile()))
        assertThat(properties.getProperty('foo')).isEqualTo('bar')
    }

    // raspbian-config-boot tests

    @Test
    void shouldPropertyToRaspianBootConfig() {
        // When
        configCommand('raspbian-config-boot', 'foo bar')

        // Then
        def properties = new Properties()
        properties.load(new FileInputStream(Paths.get(device.root().absolutePath, 'boot', 'config.txt').toFile()))
        assertThat(properties.getProperty('foo')).isEqualTo('bar')
    }

}
