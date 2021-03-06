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
package org.psikeds.resolutionengine.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.commons.lang.StringUtils;

import org.psikeds.resolutionengine.datalayer.knowledgebase.KnowledgeBase;
import org.psikeds.resolutionengine.datalayer.vo.Event;
import org.psikeds.resolutionengine.interfaces.pojos.Concept;
import org.psikeds.resolutionengine.interfaces.pojos.ConceptChoice;
import org.psikeds.resolutionengine.interfaces.pojos.ConceptChoices;
import org.psikeds.resolutionengine.interfaces.pojos.Concepts;
import org.psikeds.resolutionengine.interfaces.pojos.FeatureChoice;
import org.psikeds.resolutionengine.interfaces.pojos.FeatureChoices;
import org.psikeds.resolutionengine.interfaces.pojos.FeatureValue;
import org.psikeds.resolutionengine.interfaces.pojos.FeatureValues;
import org.psikeds.resolutionengine.interfaces.pojos.KnowledgeEntity;
import org.psikeds.resolutionengine.interfaces.pojos.Purpose;
import org.psikeds.resolutionengine.interfaces.pojos.Variant;
import org.psikeds.resolutionengine.interfaces.pojos.VariantChoice;
import org.psikeds.resolutionengine.interfaces.pojos.VariantChoices;
import org.psikeds.resolutionengine.interfaces.pojos.Variants;
import org.psikeds.resolutionengine.resolver.ResolutionException;
import org.psikeds.resolutionengine.transformer.Transformer;

/**
 * Helper for handling/manipulating all Kind of Choices
 * 
 * @author marco@juliano.de
 * 
 */
public abstract class ChoicesHelper {

  private static final Logger LOGGER = LoggerFactory.getLogger(ChoicesHelper.class);

  private ChoicesHelper() {
    // prevent instantiation
  }

  // ----------------------------------------------------------------

  public static boolean cleanupChoices(final KnowledgeBase kb, final Event e, final KnowledgeEntity ke) {
    boolean removed = false;
    try {
      LOGGER.trace("--> cleanupChoices(); E = {}; KE = {}", e.getEventID(), shortDisplayKE(ke));
      if (Event.TRIGGER_TYPE_VARIANT.equals(e.getTriggerType())) {
        removed = cleanupVariantChoices(e, ke, null);
      }
      else {
        removed = cleanupFeatureOrConceptChoices(kb, e, ke, null);
      }
      return removed;
    }
    finally {
      LOGGER.trace("<-- cleanupChoices(); E = {}; removed = {}", e.getEventID(), shortDisplayKE(ke));
    }
  }

  public static boolean cleanupVariantChoices(final Event e, final KnowledgeEntity ke, final String purposeId) {
    boolean removed = false;
    try {
      LOGGER.trace("--> cleanupVariantChoices(); PID = {}; TID = {}; E = {}; KE = {}", purposeId, e.getTriggerID(), e.getEventID(), shortDisplayKE(ke));
      if (Event.TRIGGER_TYPE_VARIANT.equals(e.getTriggerType())) {
        removed = ChoicesHelper.cleanupVariantChoices(ke, purposeId, e.getTriggerID());
        return removed;
      }
      else {
        throw new ResolutionException("Cannot cleanup Variant-Choices. Unexpected Trigger-Type: " + e.getTriggerType());
      }
    }
    finally {
      LOGGER.trace("<-- cleanupVariantChoices(); PID = {}; TID = {}, E = {}; removed = {}", purposeId, e.getTriggerID(), e.getEventID(), removed);
    }
  }

  public static boolean cleanupFeatureOrConceptChoices(final KnowledgeBase kb, final Event e, final KnowledgeEntity ke, final String variantId) {
    boolean removed = false;
    try {
      LOGGER.trace("--> cleanupFeatureOrConceptChoices(); VID = {}; TID = {}, E = {}; KE = {}", variantId, e.getTriggerID(), e.getEventID(), shortDisplayKE(ke));
      if (Event.TRIGGER_TYPE_FEATURE_VALUE.equals(e.getTriggerType())) {
        final String featureValueId = e.getTriggerID();
        final org.psikeds.resolutionengine.datalayer.vo.FeatureValue fv = kb.getFeatureValue(featureValueId);
        final String featureId = fv.getFeatureID();
        LOGGER.debug("Removing Feature-Value {} of Feature {} from Feature-Choices of KE {}", featureValueId, featureId, shortDisplayKE(ke));
        if (ChoicesHelper.cleanupFeatureChoices(ke, variantId, featureId, featureValueId)) {
          removed = true;
        }
        LOGGER.debug("Removing Feature-Value {} of Feature {} from Concept-Choices of KE {}", featureValueId, featureId, shortDisplayKE(ke));
        final FeatureValues vals = new FeatureValues();
        final FeatureValue val = new FeatureValue(fv.getFeatureID(), fv.getFeatureValueID(), fv.getValue());
        vals.add(val);
        if (cleanupConceptChoices(ke, featureId, vals, false)) {
          removed = true;
        }
        return removed;
      }
      else if (Event.TRIGGER_TYPE_CONCEPT.equals(e.getTriggerType())) {
        // currently there are only primary denotions of concepts
        // therefore we always remove all concept choices of a ke
        removed = cleanupConceptChoices(ke, variantId);
        return removed;
      }
      else {
        throw new ResolutionException("Cannot cleanup Feature-Choices. Unexpected Trigger-Type: " + e.getTriggerType());
      }
    }
    finally {
      LOGGER.trace("<-- cleanupFeatureOrConceptChoices(); VID = {}; TID = {}, E = {}; removed = {}", variantId, e.getTriggerID(), e.getEventID(), removed);
    }
  }

  // ----------------------------------------------------------------

  public static boolean cleanupVariantChoices(final KnowledgeEntity ke, final String purposeId) {
    return cleanupVariantChoices(ke, purposeId, null);
  }

  public static boolean cleanupVariantChoices(final KnowledgeEntity ke, final String purposeId, final String variantId) {
    boolean removed = false;
    try {
      LOGGER.trace("--> cleanupVariantChoices(); PID = {}; VID = {}; KE = {}", purposeId, variantId, shortDisplayKE(ke));
      final VariantChoices choices = ke.getPossibleVariants();
      final Iterator<VariantChoice> vciter = choices.iterator();
      while (vciter.hasNext()) {
        final VariantChoice vc = vciter.next();
        final Purpose p = vc.getPurpose();
        final String pid = (p == null ? null : p.getPurposeID());
        if (StringUtils.isEmpty(purposeId) || purposeId.equals(pid)) {
          final Variants variants = vc.getVariants();
          final Iterator<Variant> viter = variants.iterator();
          while (viter.hasNext()) {
            final Variant v = viter.next();
            final String vid = (v == null ? null : v.getVariantID());
            if (StringUtils.isEmpty(variantId) || variantId.equals(vid)) {
              LOGGER.debug("Removing Variant {} from Choices for Purpose {}", vid, pid);
              viter.remove();
              removed = true;
            }
          }
        }
      }
      return removed;
    }
    finally {
      LOGGER.trace("<-- cleanupVariantChoices(); PID = {}; VID = {}; removed = {}", purposeId, variantId, removed);
    }
  }

  // ----------------------------------------------------------------

  public static boolean cleanupFeatureChoices(final KnowledgeEntity ke, final String variantId, final String featureId) {
    return cleanupFeatureChoices(ke, variantId, featureId, null);
  }

  public static boolean cleanupFeatureChoices(final KnowledgeEntity ke, final String variantId, final String featureId, final String featureValueId) {
    boolean removed = false;
    try {
      LOGGER.trace("--> cleanupFeatureChoices(); VID = {}; FID = {}; FVID = {}; KE = {}", variantId, featureId, featureValueId, shortDisplayKE(ke));
      final FeatureChoices choices = ke.getPossibleFeatures();
      final Iterator<FeatureChoice> fciter = choices.iterator();
      while (fciter.hasNext()) {
        final FeatureChoice fc = fciter.next();
        final String vid = fc.getParentVariantID();
        if (StringUtils.isEmpty(variantId) || variantId.equals(vid)) {
          final String fid = fc.getFeatureID();
          if (StringUtils.isEmpty(featureId) || featureId.equals(fid)) {
            final FeatureValues values = fc.getPossibleValues();
            final Iterator<FeatureValue> valiter = values.iterator();
            while (valiter.hasNext()) {
              final FeatureValue fv = valiter.next();
              final String fvid = (fv == null ? null : fv.getFeatureValueID());
              if (StringUtils.isEmpty(featureValueId) || featureValueId.equals(fvid)) {
                LOGGER.debug("Removing FeatureValue {} of Feature {} from Choices for Variant {}", fvid, fid, vid);
                valiter.remove();
                removed = true;
              }
            }
          }
        }
      }
      return removed;
    }
    finally {
      LOGGER.trace("<-- cleanupFeatureChoices(); VID = {}; FID = {}; FVID = {}; removed = {}", variantId, featureId, featureValueId, removed);
    }
  }

  public static boolean cleanupFeatureChoices(final KnowledgeEntity ke, final String featureId, final FeatureValues vals, final boolean keep) {
    boolean removed = false;
    final int num = (vals == null ? 0 : vals.size());
    try {
      LOGGER.trace("--> cleanupFeatureChoices(); keep = {}; #Values = {}; Feature = {}; KE = {}", keep, num, featureId, shortDisplayKE(ke));
      if (ke != null) {
        final FeatureChoices choices = ke.getPossibleFeatures();
        final Iterator<FeatureChoice> fciter = choices.iterator();
        while (fciter.hasNext()) {
          final FeatureChoice fc = fciter.next();
          final String fid = fc.getFeatureID();
          // touch values/choices of the required feature only
          if (StringUtils.isEmpty(featureId) || featureId.equals(fid)) {
            final FeatureValues values = fc.getPossibleValues();
            final Iterator<FeatureValue> valiter = values.iterator();
            while (valiter.hasNext()) {
              final FeatureValue fv = valiter.next();
              if (keep) {
                if ((num <= 0) || !vals.contains(fv)) {
                  LOGGER.debug("Removing FeatureValue {} of Feature {} from Choices for Variant {}", fv.getFeatureValueID(), fv.getFeatureID(), fc.getParentVariantID());
                  valiter.remove();
                  removed = true;
                }
              }
              else {
                if ((num > 0) && vals.contains(fv)) {
                  LOGGER.debug("Removing FeatureValue {} of Feature {} from Choices for Variant {}", fv.getFeatureValueID(), fv.getFeatureID(), fc.getParentVariantID());
                  valiter.remove();
                  removed = true;
                }
              }
            }
          }
        }
      }
      return removed;
    }
    finally {
      LOGGER.trace("<-- cleanupFeatureChoices(); keep = {}; #Values = {}; Feature = {}; removed = {}\nResulting KE = {}", keep, num, featureId, removed, ke);
    }
  }

  // ----------------------------------------------------------------

  public static boolean cleanupConceptChoices(final KnowledgeEntity ke, final String variantId) {
    return cleanupConceptChoices(ke, variantId, (List<String>) null);
  }

  public static boolean cleanupConceptChoices(final KnowledgeEntity ke, final String variantId, final String conceptId) {
    List<String> conceptIDs = null;
    if (!StringUtils.isEmpty(conceptId)) {
      conceptIDs = new ArrayList<String>();
      conceptIDs.add(conceptId);
    }
    return cleanupConceptChoices(ke, variantId, conceptIDs);
  }

  public static boolean cleanupConceptChoices(final KnowledgeEntity ke, final String variantId, final List<String> conceptIDs) {
    boolean removed = false;
    try {
      LOGGER.trace("--> cleanupConceptChoices(); VID = {}; CIDs = {}; KE = {}", variantId, conceptIDs, shortDisplayKE(ke));
      if (ke != null) {
        final ConceptChoices choices = ke.getPossibleConcepts();
        final Iterator<ConceptChoice> cciter = choices.iterator();
        while (cciter.hasNext()) {
          final ConceptChoice cc = cciter.next();
          final String vid = cc.getParentVariantID();
          if (StringUtils.isEmpty(variantId) || variantId.equals(vid)) {
            final Concepts cons = cc.getConcepts();
            final Iterator<Concept> coniter = cons.iterator();
            while (coniter.hasNext()) {
              final Concept c = coniter.next();
              final String cid = (c == null ? null : c.getConceptID());
              if ((conceptIDs == null) || conceptIDs.isEmpty() || conceptIDs.contains(cid)) {
                LOGGER.debug("Removing Concept {} from Choices for Variant {}", cid, vid);
                coniter.remove();
                removed = true;
              }
            }
          }
        }
      }
      return removed;
    }
    finally {
      LOGGER.trace("<-- cleanupConceptChoices(); VID = {}; CIDs = {}; removed = {}", variantId, conceptIDs, removed);
    }
  }

  public static boolean cleanupConceptChoices(final KnowledgeEntity ke, final String featureId, final FeatureValues vals, final boolean keep) {
    boolean removed = false;
    final int num = (vals == null ? 0 : vals.size());
    try {
      LOGGER.trace("--> cleanupConceptChoices();  keep = {}; #Values = {}; Feature = {}; KE = {}", keep, num, featureId, shortDisplayKE(ke));
      if (ke != null) {
        final ConceptChoices choices = ke.getPossibleConcepts();
        final Iterator<ConceptChoice> cciter = choices.iterator();
        while (cciter.hasNext()) {
          final ConceptChoice cc = cciter.next();
          final Concepts cons = cc.getConcepts();
          final Iterator<Concept> coniter = cons.iterator();
          while (coniter.hasNext()) {
            boolean foundFeature = false;
            boolean foundValue = false;
            final Concept c = coniter.next();
            final FeatureValues feats = c.getValues();
            final Iterator<FeatureValue> fviter = feats.iterator();
            while (fviter.hasNext()) {
              final FeatureValue fv = fviter.next();
              final String fid = fv.getFeatureID();
              if (StringUtils.isEmpty(featureId) || featureId.equals(fid)) {
                foundFeature = true;
                if ((num > 0) && vals.contains(fv)) {
                  LOGGER.debug("Concept {} includes a matching Feature-Value: {}", c.getConceptID(), fv);
                  foundValue = true;
                }
              }
            }
            // touch concepts including the required feature only
            if (foundFeature && ((keep && !foundValue) || (!keep && foundValue))) {
              LOGGER.debug("Removing Concept {} from Choices for Variant {}", c.getConceptID(), cc.getParentVariantID());
              coniter.remove();
              removed = true;
            }
          }
        }
      }
      return removed;
    }
    finally {
      LOGGER.trace("<-- cleanupConceptChoices(); keep = {}; #Values = {}; Feature = {}; removed = {}\nResulting KE = {}", keep, num, featureId, removed, ke);
    }
  }

  // ----------------------------------------------------------------

  public static VariantChoices getNewVariantChoices(final KnowledgeBase kb, final Transformer trans, final org.psikeds.resolutionengine.datalayer.vo.Variant parentVariant) {
    final VariantChoices choices = new VariantChoices();
    final String parentVariantID = (parentVariant == null ? null : parentVariant.getVariantID());
    try {
      LOGGER.trace("--> getNewVariantChoices(); Variant = {}", parentVariantID);
      // get all components/purposes constituting parent-variant ...
      final org.psikeds.resolutionengine.datalayer.vo.Constitutes consts = kb.getConstitutes(parentVariantID);
      final List<org.psikeds.resolutionengine.datalayer.vo.Component> comps = (consts == null ? null : consts.getComponents());
      if ((comps != null) && !comps.isEmpty()) {
        // ... and create for every existing component/purpose ...
        for (final org.psikeds.resolutionengine.datalayer.vo.Component c : comps) {
          if (c != null) {
            final long qty = c.getQuantity();
            final String pid = c.getPurposeID();
            final org.psikeds.resolutionengine.datalayer.vo.Purpose p = kb.getPurpose(pid);
            // ... a new VariantChoice-POJO for the Client
            final org.psikeds.resolutionengine.datalayer.vo.Fulfills ff = kb.getFulfills(pid);
            final org.psikeds.resolutionengine.datalayer.vo.Variants variants = new org.psikeds.resolutionengine.datalayer.vo.Variants();
            for (final String vid : ff.getVariantID()) {
              variants.addVariant(kb.getVariant(vid));
            }
            final VariantChoice vc = trans.valueObject2Pojo(parentVariantID, p, variants, qty);
            LOGGER.debug("Adding new Variant-Choice: {}", vc);
            choices.add(vc);
          }
        }
      }
      // return list of all new variant choices
      return choices;
    }
    finally {
      LOGGER.trace("<-- getNewVariantChoices(); Variant = {}\nChoices = {}", parentVariantID, choices);
    }
  }

  public static FeatureChoices getNewFeatureChoices(final KnowledgeBase kb, final Transformer trans, final org.psikeds.resolutionengine.datalayer.vo.Variant parentVariant) {
    final FeatureChoices choices = new FeatureChoices();
    final String parentVariantID = (parentVariant == null ? null : parentVariant.getVariantID());
    try {
      LOGGER.trace("--> getNewFeatureChoices(); Variant = {}", parentVariantID);
      for (final String featureId : parentVariant.getFeatureIds()) {
        // get all values allowed for this feature on this variant
        final FeatureValues values = trans.valueObject2Pojo(kb.getFeatureValues(parentVariantID, featureId));
        if ((values != null) && !values.isEmpty()) {
          final FeatureChoice fc = new FeatureChoice(parentVariantID, featureId, values);
          LOGGER.debug("Adding new Feature-Choice: {}", fc);
          choices.add(fc);
        }
      }
      // return list of all new feature choices
      return choices;
    }
    finally {
      LOGGER.trace("<-- getNewFeatureChoices()\nChoices = {}", choices);
    }
  }

  public static ConceptChoices getNewConceptChoices(final KnowledgeBase kb, final Transformer trans, final org.psikeds.resolutionengine.datalayer.vo.Variant parentVariant) {
    final ConceptChoices choices = new ConceptChoices();
    final String parentVariantID = (parentVariant == null ? null : parentVariant.getVariantID());
    try {
      LOGGER.trace("--> getNewConceptChoices(); Variant = {}", parentVariantID);
      final Concepts cons = trans.valueObject2Pojo(kb.getAttachedConcepts(parentVariantID));
      if ((cons != null) && !cons.isEmpty()) {
        // currently we have only variants that are primarily denoted by concepts, i.e. there
        // is always just one choice where exactly one of the concepts can/must be selected.
        final ConceptChoice cc = new ConceptChoice(parentVariantID, cons);
        choices.add(cc);
      }
      // return new concept choices
      return choices;
    }
    finally {
      LOGGER.trace("<-- getNewConceptChoices()\nChoices = {}", choices);
    }
  }

  // ----------------------------------------------------------------

  public static FeatureValues getFeatureValues(final KnowledgeEntity ke, final String featureId) {
    FeatureValues result = null;
    try {
      LOGGER.trace("--> getFeatureValues(); KE = {}; featureId = {}", shortDisplayKE(ke), featureId);
      if ((ke != null) && !StringUtils.isEmpty(featureId)) {
        for (final FeatureChoice fc : ke.getPossibleFeatures()) {
          if (featureId.equals(fc.getFeatureID())) {
            // there is at most one choice for a feature
            result = fc.getPossibleValues();
            if (!LOGGER.isTraceEnabled()) {
              LOGGER.debug("Possible Feature Values of KE {} for Feature {}", shortDisplayKE(ke), featureId, result);
            }
            return result;
          }
        }
      }
      return result;
    }
    finally {
      LOGGER.trace("<-- getFeatureValues(); KE = {}; featureId = {}\nResult = {}", shortDisplayKE(ke), featureId, result);
    }
  }

  public static FeatureValues getFeatureValues(final Concepts concepts, final String featureId) {
    FeatureValues result = null;
    try {
      LOGGER.trace("--> getFeatureValues(); featureId = {}; Concepts = {}", featureId, concepts);
      if ((concepts != null) && !StringUtils.isEmpty(featureId)) {
        for (final Concept con : concepts) {
          LOGGER.debug("Checking Concept {} regarding Feature {}", con.getConceptID(), featureId);
          for (final FeatureValue fv : con.getValues()) {
            if (featureId.equals(fv.getFeatureID())) {
              LOGGER.debug("Feature-Value {} of Concept {} is relevant regarding Feature {}", fv.getFeatureValueID(), con.getConceptID(), fv.getFeatureID());
              if (result == null) {
                result = new FeatureValues();
              }
              if (!result.contains(fv)) {
                result.add(fv);
              }
            }
          }
        }
      }
      if (!LOGGER.isTraceEnabled()) {
        LOGGER.debug("Found Feature-Values: {}", result);
      }
      return result;
    }
    finally {
      LOGGER.trace("<-- getFeatureValues(); featureId = {}; Result = {}", featureId, result);
    }
  }

  public static Concepts getConcepts(final KnowledgeEntity ke, final String featureId) {
    Concepts result = null;
    try {
      LOGGER.trace("--> getConcepts(); KE = {}; featureId = {}", shortDisplayKE(ke), featureId);
      if ((ke != null) && !StringUtils.isEmpty(featureId)) {
        for (final ConceptChoice cc : ke.getPossibleConcepts()) {
          final Concepts concepts = cc.getConcepts();
          for (final Concept con : concepts) {
            LOGGER.debug("Checking Concept {} of KE {} regarding Feature {}", con.getConceptID(), shortDisplayKE(ke), featureId);
            for (final FeatureValue fv : con.getValues()) {
              if (featureId.equals(fv.getFeatureID())) {
                LOGGER.debug("Feature-Value {} of Concept {} is relevant regarding Feature {}", fv.getFeatureValueID(), con.getConceptID(), fv.getFeatureID());
                if (result == null) {
                  result = new Concepts();
                }
                if (!result.contains(con)) {
                  result.add(con);
                }
              }
            }
          }
        }
      }
      if (!LOGGER.isTraceEnabled()) {
        LOGGER.debug("Found Concepts: {}", result);
      }
      return result;
    }
    finally {
      LOGGER.trace("<-- getConcepts(); KE = {}; featureId = {}\nResult = {}", shortDisplayKE(ke), featureId, result);
    }
  }

  // ----------------------------------------------------------------

  public static String shortDisplayKE(final KnowledgeEntity ke) {
    return KnowledgeEntityHelper.shortDisplayKE(ke);
  }
}
