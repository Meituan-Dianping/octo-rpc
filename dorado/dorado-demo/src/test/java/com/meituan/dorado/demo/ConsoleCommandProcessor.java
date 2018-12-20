/*
 * Copyright 2018 Meituan Dianping. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.meituan.dorado.demo;

import com.meituan.dorado.bootstrap.ServiceBootstrap;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Arrays;

public class ConsoleCommandProcessor {

    public static void processCommands(ClassPathXmlApplicationContext beanFactory) throws Exception {
        processCommandsWithQuitOperation(beanFactory, null);
    }

    public static void processCommandsWithQuitOperation(ClassPathXmlApplicationContext beanFactory, QuitOperation quitOperation) throws Exception {
        printHelp();
        BufferedReader in = null;
        InputStreamReader inputStreamReader = null;
        try {
            inputStreamReader = new InputStreamReader(System.in);
            in = new BufferedReader(inputStreamReader);
            boolean done = false;
            while (!done) {
                System.out.print("> ");
                String line = in.readLine();
                if (line == null) {
                    break;
                }

                String command = line.trim();
                String[] parts = command.split("\\s");
                if (parts.length == 0) {
                    continue;
                }
                String operation = parts[0];
                String args[] = Arrays.copyOfRange(parts, 1, parts.length);
                if (operation.equalsIgnoreCase("q") || operation.equalsIgnoreCase("quit")) {
                    done = true;
                }
            }
            beanFactory.destroy();
            ServiceBootstrap.clearGlobalResource();
            if (quitOperation != null) {
                quitOperation.prepareQuit();
            }
            System.exit(0);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (in != null) {
                in.close();
            }
            if (inputStreamReader != null) {
                inputStreamReader.close();
            }
        }
    }

    private static void printHelp() {
        System.out.println("An example of using Dorado demo. This example is driven by entering commands at the prompt:\n");
        System.out.println("quit or q: Quit the example");
    }
}
