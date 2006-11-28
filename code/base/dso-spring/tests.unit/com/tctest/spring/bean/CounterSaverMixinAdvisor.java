/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */

package com.tctest.spring.bean;

import org.springframework.aop.support.DefaultIntroductionAdvisor;

public class CounterSaverMixinAdvisor extends DefaultIntroductionAdvisor {

    public CounterSaverMixinAdvisor() {
        super(new CounterSaverMixin(), CounterSaver.class);
    }

}

