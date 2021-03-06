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

import javax.xml.bind.annotation.XmlSeeAlso;

import org.codehaus.jackson.annotate.JsonSubTypes;

/**
 * A general Decission, either VariantDecission, FeatureDecission or ConceptDecission
 * 
 * @author marco@juliano.de
 * 
 */
@XmlSeeAlso({ ConceptDecission.class, FeatureDecission.class, VariantDecission.class })
@JsonSubTypes({ @JsonSubTypes.Type(value = ConceptDecission.class, name = "ConceptDecission"),
    @JsonSubTypes.Type(value = FeatureDecission.class, name = "FeatureDecission"),
    @JsonSubTypes.Type(value = VariantDecission.class, name = "VariantDecission"), })
public abstract class Decission extends POJO {

  private static final long serialVersionUID = 1L;

  public Decission() {
    super();
  }

  public Decission(final POJO... pojos) {
    super(pojos);
  }

  public Decission(final String... ids) {
    super(ids);
  }

  public Decission(final String id) {
    super(id);
  }
}
