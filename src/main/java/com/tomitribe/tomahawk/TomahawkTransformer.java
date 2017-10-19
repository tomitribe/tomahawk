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

import javassist.ByteArrayClassPath;
import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtMethod;
import javassist.Modifier;
import javassist.NotFoundException;

public class TomahawkTransformer implements ClassFileTransformer {
    private static final String HANDSHAKER_INVOKE_LOGGER =
        "printLogLine(\"CLIENT_RANDOM\", $0.clnt_random.random_bytes, $0.session.getMasterSecret().getEncoded());";
    private static final String RSA_KEY_EXCHANGE_INVOKE_LOGGER = "printLogLine(\"RSA\", $0.encrypted, $0.preMaster.getEncoded());";
    // @formatter:off
    private static final String PRINT_LOG_LINE_METHOD_BODY =
              "{ "
            + "String logLine = $1 + \" \" + toHexString($2) + \" \" + toHexString($3);"
            + "System.out.println(logLine);"
            + "return;"
            + "}";
    private static final String TO_HEX_STIRNG_METHOD_BODY =
             "{ "
            + "StringBuilder hexString = new StringBuilder();"
            + "    for (int position = 0; position < $1.length; position++) {"
            + "        int currentByte = $1[position] & 0xFF;"
            + "        hexString.append(currentByte < 0x10 ? \"0\" : \"\");"
            + "        hexString.append(Integer.toHexString(currentByte));"
            + "    }"
            + "return hexString.toString();"
            + "}";
    // @formatter:on
    private static final String RSA_KEY_EXCHANGE = "sun/security/ssl/RSAClientKeyExchange";
    private static final String HANDSHAKER = "sun/security/ssl/Handshaker";

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain,
                            byte[] classfileBuffer) throws IllegalClassFormatException {
        try {
            byte[] classDef = classfileBuffer;
            if (className != null) {
                switch (className) {
                    case HANDSHAKER:
                        classDef = instrumentHandshaker(classfileBuffer, className.replaceAll("/", "."));
                        break;
                    case RSA_KEY_EXCHANGE:
                        classDef = instrumentExchange(classfileBuffer, className.replaceAll("/", "."));
                        break;
                }
            }
            return classDef;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private byte[] instrumentHandshaker(byte[] classfileBuffer, String className) throws CannotCompileException, IOException,
                                                                                  NotFoundException {
        System.out.println("Attempting to enhance " + className + "...");
        ClassPool cp = getClassPool(classfileBuffer, className);
        CtClass cc = cp.get(className);
        insert_toHexString(cp, cc);
        insert_printLogLine(cp, cc);
        appendTo_calculateKeys(cc);
        cc.freeze();
        return cc.toBytecode();
    }

    private byte[] instrumentExchange(byte[] classfileBuffer, String className) throws CannotCompileException, IOException,
                                                                                NotFoundException {
        System.out.println("Attempting to enhance " + className + "...");
        ClassPool cp = getClassPool(classfileBuffer, className);
        CtClass cc = cp.get(className);
        insert_toHexString(cp, cc);
        insert_printLogLine(cp, cc);
        appendTo_constructors(cc);
        cc.freeze();
        return cc.toBytecode();
    }

    private ClassPool getClassPool(byte[] classfileBuffer, String className) {
        ClassPool cp = ClassPool.getDefault();
        cp.insertClassPath(new ByteArrayClassPath(className, classfileBuffer));
        return cp;
    }

    private void insert_toHexString(ClassPool cp, CtClass cc) throws NotFoundException, CannotCompileException {
        CtMethod insertMethod =
            new CtMethod(cp.get(String.class.getName()), "toHexString", new CtClass[] { cp.get(byte[].class.getName()) }, cc);
        insertMethod.setModifiers(Modifier.PRIVATE | Modifier.STATIC | Modifier.FINAL);
        insertMethod.setBody(TO_HEX_STIRNG_METHOD_BODY);
        cc.addMethod(insertMethod);
    }

    private void insert_printLogLine(ClassPool cp, CtClass cc) throws NotFoundException, CannotCompileException {
        CtMethod insertMethod = new CtMethod(cp.get(Void.class.getName()), "printLogLine", new CtClass[] {
            cp.get(String.class.getName()), cp.get(byte[].class.getName()), cp.get(byte[].class.getName()) }, cc);
        insertMethod.setModifiers(Modifier.PRIVATE | Modifier.STATIC | Modifier.FINAL);
        insertMethod.setBody(PRINT_LOG_LINE_METHOD_BODY);
        cc.addMethod(insertMethod);
    }

    private void appendTo_calculateKeys(CtClass cc) throws NotFoundException, CannotCompileException {
        CtMethod calculateKeysMethod = cc.getDeclaredMethod("calculateKeys");
        calculateKeysMethod.insertAfter(HANDSHAKER_INVOKE_LOGGER);
    }

    private void appendTo_constructors(CtClass cc) throws CannotCompileException {
        CtConstructor[] declaredConstructors = cc.getDeclaredConstructors();
        for (CtConstructor con : declaredConstructors) {
            con.insertAfter(RSA_KEY_EXCHANGE_INVOKE_LOGGER);
        }
    }
}
