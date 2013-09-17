/*******************************************************************************
 * psiKeds :- ps induced knowledge entity delivery system
 *
 * Copyright (c) 2013, 2014 Karsten Reincke, Marco Juliano, Deutsche Telekom AG
 *
 * This file is free software: you can redistribute
 * it and/or modify it under the terms of the
 * [ ] GNU Affero General Public License
 * [ ] GNU General Public License
 * [x] GNU Lesser General Public License
 * [ ] Creatice Commons ShareAlike License
 *
 * For details see file LICENSING in the top project directory
 *******************************************************************************/
package org.psikeds.queryagent.interfaces.presenter.pojos;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Interface object representing a List of all Constituents,
 * i.e. of all Constitutes-Relations.
 *
 * Note: Reading from and writing to JSON works out of the box.
 *       However for XML the XmlRootElement annotation is required.
 *
 * @author marco@juliano.de
 *
 */
@XmlRootElement(name = "Constituents")
public class Constituents extends POJO implements Serializable {

  private static final long serialVersionUID = 1L;

  private List<Constitutes> constitutes;

  public Constituents() {
    this(null);
  }

  public Constituents(final List<Constitutes> constitutes) {
    super();
    setConstitutes(constitutes);
  }

  public List<Constitutes> getConstitutes() {
    if (this.constitutes == null) {
      this.constitutes = new ArrayList<Constitutes>();
    }
    return this.constitutes;
  }

  public boolean addConstitutes(final Constitutes value) {
    return getConstitutes().add(value);
  }

  public void setConstitutes(final List<Constitutes> lst) {
    this.constitutes = lst;
  }
}
