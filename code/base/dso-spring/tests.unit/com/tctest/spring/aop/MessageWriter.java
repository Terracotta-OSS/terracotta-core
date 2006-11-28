package com.tctest.spring.aop;

public class MessageWriter implements IMessageWriter{
    public String writeMessage() {
        return "World";
    }
}