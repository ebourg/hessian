/*
 * Copyright (c) 2001-2004 Caucho Technology, Inc.  All rights reserved.
 *
 * The Apache Software License, Version 1.1
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Caucho Technology (http://www.caucho.com/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "Hessian", "Resin", and "Caucho" must not be used to
 *    endorse or promote products derived from this software without prior
 *    written permission. For written permission, please contact
 *    info@caucho.com.
 *
 * 5. Products derived from this software may not be called "Resin"
 *    nor may "Resin" appear in their names without prior written
 *    permission of Caucho Technology.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL CAUCHO TECHNOLOGY OR ITS CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY,
 * OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT
 * OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 * IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * @author Scott Ferguson
 */

package com.caucho.hessian.io;

import java.io.*;
import java.util.HashMap;

/**
 * Deserializing a JDK 1.4 StackTraceElement
 */
public class StackTraceElementDeserializer extends AbstractMapDeserializer {
  private static Class _stackTraceClass;
  
  public StackTraceElementDeserializer()
  {
  }
  
  public Class getType()
  {
    return _stackTraceClass;
  }
  
  public Object readMap(AbstractHessianInput in)
    throws IOException
  {
    HashMap map = new HashMap();

    in.addRef(map);

    String declaringClass = null;
    String methodName = null;
    String fileName = null;
    int lineNumber = 0;

    while (! in.isEnd()) {
      String key = in.readString();

      if (key.equals("declaringClass"))
        declaringClass = in.readString();
      else if (key.equals("methodName"))
        methodName = in.readString();
      else if (key.equals("fileName"))
        fileName = in.readString();
      else if (key.equals("lineNumber"))
        lineNumber = in.readInt();
      else {
        Object value = in.readObject();
      }
    }

    in.readMapEnd();

    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    DataOutputStream dos = new DataOutputStream(bos);
    ObjectOutputStream oos = new ObjectOutputStream(dos);

    oos.writeObject(_stackTraceClass);

    Throwable e1 = new IOException();
    try {
      throw e1;
    } catch (Throwable e2) {
      e1 = e2;
    }

    dos.writeByte(ObjectStreamConstants.TC_OBJECT);
    dos.writeByte(ObjectStreamConstants.TC_REFERENCE);
    dos.writeShort(0x007e);
    dos.writeShort(0);
    
    dos.writeInt(lineNumber);
    
    if (declaringClass != null) {
      dos.writeByte(ObjectStreamConstants.TC_STRING);
      dos.writeUTF(declaringClass);
    }
    else
      dos.writeByte(ObjectStreamConstants.TC_NULL);
    
    if (fileName != null) {
      dos.writeByte(ObjectStreamConstants.TC_STRING);
      dos.writeUTF(fileName);
    }
    else
      dos.writeByte(ObjectStreamConstants.TC_NULL);
    
    if (methodName != null) {
      dos.writeByte(ObjectStreamConstants.TC_STRING);
      dos.writeUTF(methodName);
    }
    else
      dos.writeByte(ObjectStreamConstants.TC_NULL);

    oos.close();
    dos.close();
    bos.close();

    byte []data = bos.toByteArray();

    try {
      ByteArrayInputStream bis = new ByteArrayInputStream(data);
      ObjectInputStream ois = new ObjectInputStream(bis);

      Object cl = ois.readObject();

      Object obj = ois.readObject();
      
      ois.close();
      bis.close();
      
      return obj;
    } catch (Throwable e) {
      return null;
    }
  }

  static {
    try {
      _stackTraceClass = Class.forName("java.lang.StackTraceElement");
    } catch (Throwable e) {
    }
  }
}
