/*******************************************************************************
 * psiKeds :- ps induced knowledge entity delivery system
 *
 * Copyright (c) 2013, 2014 Karsten Reincke, Marco Juliano, Deutsche Telekom AG
 *
 * This file is free software: you can redistribute
 * it and/or modify it under the terms of the
 * [x] GNU Affero General Public License
 * [ ] GNU General Public License
 * [ ] GNU Lesser General Public License
 * [ ] Creatice Commons ShareAlike License
 *
 * For details see file LICENSING in the top project directory
 *******************************************************************************/
package org.psikeds.resolutionengine.interfaces.pojos;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;

import javax.xml.bind.annotation.XmlRootElement;

import org.codehaus.jackson.annotate.JsonTypeInfo;

/**
 * A List of Variants ... unfortunately we have to create a Sub-Class of
 * ArrayList<Variant> because a simple List will loose its Type-Information
 * due to Java-Type-Erasure resulting in errors during JSON-Deserialization!
 * 
 * @author marco@juliano.de
 * 
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "@type")
@XmlRootElement(name = "Variants")
public class Variants extends ArrayList<Variant> implements Serializable {

  private static final long serialVersionUID = 1L;

  public Variants() {
    super();
  }

  public Variants(final Collection<? extends Variant> c) {
    super(c);
  }

  public Variants(final int initialCapacity) {
    super(initialCapacity);
  }
}