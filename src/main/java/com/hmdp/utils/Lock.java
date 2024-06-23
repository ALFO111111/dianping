package com.hmdp.utils;

/*
实现对多线程的加锁和解锁
 */
public interface Lock {
    boolean tryLock(long timeoutSec);

    void unlock();
}
