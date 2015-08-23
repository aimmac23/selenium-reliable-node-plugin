selenium-reliable-node-plugin
=============================

This is a Selenium Grid plugin that aims to make the Grid more resilient to broken nodes, by testing and isolating non-working configurations.

At the time of writing, it is possible for a new node which cannot create sessions to be introduced to the Selenium Grid, causing a large fraction of the new
Selenium jobs on the Grid to start failing. It is also possible for a previously working node to break for various reasons, also causing the same set of problems.

This plugin aims to solve this issue, by verifying that each configuration on every node (that uses this plugin) works correctly before introducing it into general use.

## Operation

This plugin implements the following core features:

 * When a new node is registered, all the configurations on that node are automatically tested to make sure they are working
 * New session requests are not allowed to use the new node without the configurations being verified as working
 * If a configuration that was previously known to work fails to create new sessions, then take it out of circulation and re-test it.

Additional Features:

 * In the "Beta" console on the Hub, broken configurations are highlighted (requires that this JAR appears on the classpath before the Selenium JAR to work).

## Setup

On the node-side, specify the "-proxy" argument to make the Hub use the correct proxy:

    java -jar selenium-server-standalone-2.40.0.jar -role node -proxy com.aimmac23.hub.proxy.ReliableProxy

On the Hub side, startup using:

    java -cp selenium-reliable-node-plugin-0.7.jar:selenium-server-standalone-2.40.0.jar org.openqa.grid.selenium.GridLauncher -role hub -servlets com.aimmac23.hub.servlet.NodeTestingServlet

## Caveats

 * If this plugins' automated testing marks a node configuration as broken, there is no way to get it back into circulation without restarting that node.
 * May not handle transient errors very well - are there any desired capabilities for a job that could cause new session requests to fail?
