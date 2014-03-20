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
package org.psikeds.resolutionengine.resolver.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.psikeds.resolutionengine.interfaces.pojos.Decission;
import org.psikeds.resolutionengine.interfaces.pojos.FeatureChoice;
import org.psikeds.resolutionengine.interfaces.pojos.FeatureChoices;
import org.psikeds.resolutionengine.interfaces.pojos.FeatureDecission;
import org.psikeds.resolutionengine.interfaces.pojos.Knowledge;
import org.psikeds.resolutionengine.interfaces.pojos.KnowledgeEntity;
import org.psikeds.resolutionengine.interfaces.pojos.Metadata;
import org.psikeds.resolutionengine.resolver.ResolutionException;
import org.psikeds.resolutionengine.resolver.Resolver;
import org.psikeds.resolutionengine.rules.RulesAndEventsHandler;

/**
 * Based on the made Feature-Decission this Resolver will reduce the corresponding
 * Feature-Choice to just one single Feature-Value so that afterwards new Knowledge-
 * Entities will be created by the AutoCompletion-Resolver.
 * If no Decission is supplied, nothing happens. Metadata is optional.
 * 
 * Note: Must run before AutoCompletion-Resolver!
 * 
 * @author marco@juliano.de
 */
public class FeatureDecissionEvaluator implements Resolver {

  private static final Logger LOGGER = LoggerFactory.getLogger(FeatureDecissionEvaluator.class);

  /**
   * @param knowledge
   *          current Knowledge
   * @param decission
   *          some Decission, ignored if not a FeatureDecission
   * @param raeh
   *          all Rules and Events currently relevant (ignored!)
   * @param metadata
   *          Metadata (optional, can be null)
   * @return Knowledge
   *         resulting new Knowledge
   * @throws ResolutionException
   *           if any error occurs
   */
  @Override
  public Knowledge resolve(final Knowledge knowledge, final Decission decission, final RulesAndEventsHandler raeh, final Metadata metadata) throws ResolutionException {
    boolean ok = false;
    boolean found = false;
    try {
      LOGGER.debug("Evaluating made Decission ...");
      if (decission instanceof FeatureDecission) {
        final FeatureDecission fd = (FeatureDecission) decission;
        found = updateKnowledge(knowledge, fd, metadata);
      }
      else {
        LOGGER.debug("Skipping Evaluation, no Feature-Decission.");
      }
      ok = true;
      return knowledge;
    }
    finally {
      LOGGER.debug("... finished evaluating made Decission: " + (found ? "" : "NOT") + " found, " + (ok ? "OK." : "ERROR!"));
    }
  }

  // ----------------------------------------------------------------

  private boolean updateKnowledge(final Knowledge knowledge, final FeatureDecission fd, final Metadata metadata) throws ResolutionException {
    boolean found = false;
    try {
      LOGGER.trace("--> updateKnowledge(); Feature-Decission = {}\nKnowledge = {}", fd, knowledge);
      for (final KnowledgeEntity ke : knowledge.getEntities()) {
        found = found | updateKnowledgeEntity(ke, fd, metadata);
      }
      return found;
    }
    finally {
      LOGGER.trace("<-- updateKnowledge(); Found = {}; Knowledge = {}", found, knowledge);
    }
  }

  private boolean updateKnowledgeEntity(final KnowledgeEntity ke, final FeatureDecission fd, final Metadata metadata) throws ResolutionException {
    boolean found = false;
    try {
      LOGGER.trace("--> updateKnowledgeEntity(); KnowledgeEntity = {}", ke);
      found = updateFeatureChoices(ke.getPossibleFeatures(), fd, metadata);
      for (final KnowledgeEntity child : ke.getChildren()) {
        found = found | updateKnowledgeEntity(child, fd, metadata);
      }
      return found;
    }
    finally {
      LOGGER.trace("<-- updateKnowledgeEntity(); Found = {}; KnowledgeEntity = {}", found, ke);
    }
  }

  private boolean updateFeatureChoices(final FeatureChoices choices, final FeatureDecission fd, final Metadata metadata) throws ResolutionException {
    boolean found = false;
    try {
      LOGGER.trace("--> updateFeatureChoices(); Feature-Decission = {}\nChoices = {}", fd, choices);
      for (final FeatureChoice fc : choices) {
        if ((fc != null) && fc.matches(fd)) {
          // Found Feature-Value matching our Decission!
          fc.setValue(fd.getFeatureValue());
          found = true;
          decissionMessage(metadata, fd, fc);
        }
      }
      return found;
    }
    finally {
      LOGGER.trace("<-- updateFeatureChoices(); Found = {}; Choices = {}", found, choices);
    }
  }

  private void decissionMessage(final Metadata metadata, final FeatureDecission fd, final FeatureChoice fc) {
    final String msg = String.format("Found Feature-Choice matching Decission.\nDecission = %s\nChoice = %s", fd, fc);
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug(msg);
    }
    else {
      LOGGER.info("Applying Feature-Decission {}", fd);
    }
    if (metadata != null) {
      final String key = String.format("Decission_FD_%s_%s", fd.getVariantID(), fd.getFeatureID());
      metadata.addInfo(key, msg);
    }
  }
}
