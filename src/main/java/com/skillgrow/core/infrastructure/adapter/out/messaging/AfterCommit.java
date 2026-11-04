package com.skillgrow.core.infrastructure.adapter.out.messaging;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Ejecuta una tarea cuando la transacción hace commit. Si la transacción se revierte, la tarea no corre:
 * así nunca se anuncia una sesión que no quedó guardada en Azure SQL.
 */
final class AfterCommit {

    private AfterCommit() {}

    static void run(Runnable task) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    task.run();
                }
            });
        } else {
            task.run();
        }
    }
}
