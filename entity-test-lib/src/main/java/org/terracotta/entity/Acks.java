package org.terracotta.entity;


public enum Acks {
    RECEIPT,
    PERSIST_IN_SEQUENCER,
    REPLICATED,
    APPLIED
}
