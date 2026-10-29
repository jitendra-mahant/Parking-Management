package com.parking.parking_backend;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class TestRunner {

    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println("   PARKEASE ENTERPRISE — AUTOMATED VERIFICATION & TEST SUITE");
        System.out.println("================================================================================");

        List<Class<?>> testClasses = List.of(
                com.parking.parking_backend.util.FareCalculatorTest.class,
                com.parking.parking_backend.service.SlotAvailabilityServiceTest.class,
                com.parking.parking_backend.service.AdminServiceTest.class
        );

        int totalFound = 0;
        int totalPassed = 0;
        int totalFailed = 0;
        long startTime = System.currentTimeMillis();

        for (Class<?> clazz : testClasses) {
            System.out.println("\n[SUITE] " + clazz.getSimpleName());
            System.out.println("--------------------------------------------------------------------------------");

            List<Method> beforeMethods = new ArrayList<>();
            List<Method> testMethods = new ArrayList<>();

            for (Method m : clazz.getDeclaredMethods()) {
                if (m.isAnnotationPresent(BeforeEach.class)) {
                    beforeMethods.add(m);
                }
                if (m.isAnnotationPresent(Test.class)) {
                    testMethods.add(m);
                }
            }

            for (Method testMethod : testMethods) {
                totalFound++;
                String displayName = testMethod.getName();
                if (testMethod.isAnnotationPresent(DisplayName.class)) {
                    displayName = testMethod.getAnnotation(DisplayName.class).value();
                }

                try {
                    Object testInstance = clazz.getDeclaredConstructor().newInstance();

                    // Run @BeforeEach
                    for (Method before : beforeMethods) {
                        before.setAccessible(true);
                        before.invoke(testInstance);
                    }

                    // Run @Test
                    testMethod.setAccessible(true);
                    long t0 = System.nanoTime();
                    testMethod.invoke(testInstance);
                    long nanos = System.nanoTime() - t0;

                    System.out.printf("  [PASS] %-62s (%1.2f ms)%n", displayName, nanos / 1_000_000.0);
                    totalPassed++;
                } catch (Throwable t) {
                    Throwable cause = t.getCause() != null ? t.getCause() : t;
                    System.err.printf("  [FAIL] %-62s%n", displayName);
                    System.err.println("         Error: " + cause.getClass().getSimpleName() + " - " + cause.getMessage());
                    cause.printStackTrace();
                    totalFailed++;
                }
            }
        }

        long elapsed = System.currentTimeMillis() - startTime;
        System.out.println("\n================================================================================");
        System.out.println("TEST EXECUTION SUMMARY");
        System.out.println("================================================================================");
        System.out.println("  Total Tests Found:     " + totalFound);
        System.out.println("  Tests Passed:          " + totalPassed);
        System.out.println("  Tests Failed:          " + totalFailed);
        System.out.println("  Execution Time:        " + elapsed + " ms");
        System.out.println("  System Status:         " + (totalFailed == 0 ? "ALL CHECKS PASSED (OPERATIONAL)" : "FAILURES DETECTED"));
        System.out.println("================================================================================");

        if (totalFailed > 0) {
            System.exit(1);
        }
    }
}
