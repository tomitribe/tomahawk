/*
 * Tomitribe Confidential
 *
 * Copyright Tomitribe Corporation. 2016
 *
 * The source code for this program is not published or otherwise divested of its trade secrets, irrespective of what has been
 * deposited with the U.S. Copyright Office.
 *
 */
package com.tomitribe.tomahawk;

import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.Date;

import javassist.ByteArrayClassPath;
import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtMethod;
import javassist.NotFoundException;

public class TomahawkTransformer implements ClassFileTransformer { // @formatter:off
    // @formatter:on
    private static final String HANDSHAKER_INVOKE_LOGGER =
        "com.tomitribe.tomahawk.TomahawkPrinter.queueLogLine(\"CLIENT_RANDOM\", $0.clnt_random.random_bytes, $0.session.getMasterSecret().getEncoded(), $0.conn);";
    private static final String RSA_KEY_EXCHANGE_INVOKE_LOGGER =
        "com.tomitribe.tomahawk.TomahawkPrinter.queueLogLine(\"RSA\", $0.encrypted, $0.preMaster.getEncoded(), null);";
    private static final String RSA_KEY_EXCHANGE = "sun/security/ssl/RSAClientKeyExchange";
    private static final String HANDSHAKER = "sun/security/ssl/Handshaker";

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain,
                            byte[] classfileBuffer) throws IllegalClassFormatException {
        byte[] classDef = classfileBuffer;
        try {
            if (className != null) {
                switch (className) {
                    case HANDSHAKER:
                        classDef = instrumentHandshaker(classfileBuffer, className.replaceAll("/", "."));
                        break;
                    case RSA_KEY_EXCHANGE:
                        classDef = instrumentExchange(classfileBuffer, className.replaceAll("/", "."));
                        break;
                    default:
                        break;
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return classDef;
    }

    private byte[] instrumentHandshaker(byte[] classfileBuffer, String className) throws Exception {
        logEnhance(className);
        ClassPool cp = insertIntoClassPool(classfileBuffer, className);
        CtClass cc = cp.get(className);
        appendToCalculateKeys(HANDSHAKER_INVOKE_LOGGER, cc);
        cc.freeze();
        return cc.toBytecode();
    }

    private byte[] instrumentExchange(byte[] classfileBuffer, String className) throws CannotCompileException, IOException,
                                                                                NotFoundException {
        logEnhance(className);
        ClassPool cp = insertIntoClassPool(classfileBuffer, className);
        CtClass cc = cp.get(className);
        CtClass print = cp.get(TomahawkPrinter.class.getCanonicalName());
        cc.setSuperclass(print);
        appendToConstructors(RSA_KEY_EXCHANGE_INVOKE_LOGGER, cc);
        cc.freeze();
        return cc.toBytecode();
    }

    private void logEnhance(String className) {
        System.out.println(String.format("%s TomahawkAgent: Attempting to enhance '%s'...", new Date().toString(), className));
    }

    private ClassPool insertIntoClassPool(byte[] classfileBuffer, String className) {
        ClassPool cp = ClassPool.getDefault();
        cp.insertClassPath(new ByteArrayClassPath(className, classfileBuffer));
        return cp;
    }

    private void appendToCalculateKeys(String code, CtClass cc) throws NotFoundException, CannotCompileException {
        CtMethod calculateKeysMethod = cc.getDeclaredMethod("calculateKeys");
        calculateKeysMethod.insertAfter(code);
    }

    private void appendToConstructors(String code, CtClass cc) throws CannotCompileException {
        CtConstructor[] declaredConstructors = cc.getDeclaredConstructors();
        for (CtConstructor con : declaredConstructors) {
            con.insertAfter(code);
        }
    }
}
