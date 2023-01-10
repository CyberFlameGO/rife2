/*
 * Copyright 2001-2022 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife;

import rife.engine.Server;
import rife.engine.Site;

public class HelloAll extends Site {
    public void setup() {
        group(new HelloAuthentication());
        group("/cmf", new HelloContentManagement());
        group(new HelloContinuations());
        group(new HelloCounterContinuations());
        group(new HelloDatabase());
        group(new HelloErrors());
        group(new HelloForm());
        group("/generation", new HelloFormGeneration());
        group("/continuation", new HelloFormContinuations());
        group("/generic", new HelloGenericQueryManager());
        group(new HelloGroup());
        group(new HelloLink());
        group(new HelloPathInfoMapping());
        group("/scheduler", new HelloScheduler());
        group(new HelloSvg());
        group(new HelloTemplate());
        group("/validation", new HelloValidation());
        group(new HelloWorld());

        get("/", c -> c.print(c.template("HelloAll")));
    }

    public static void main(String[] args) {
        new Server().start(new HelloAll());
    }
}
