package vars.knowledgebase.jpa;

import vars.jpa.DAO;
import vars.jpa.JPAEntity;
import vars.knowledgebase.ConceptDAO;
import vars.knowledgebase.Concept;
import vars.knowledgebase.ConceptName;
import vars.knowledgebase.ConceptNameDAO;
import vars.VARSPersistenceException;
import com.google.inject.Inject;

import java.util.Collection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.EntityManager;

/**
 * Created by IntelliJ IDEA.
 * User: brian
 * Date: Aug 7, 2009
 * Time: 4:43:41 PM
 * To change this template use File | Settings | File Templates.
 */
public class ConceptDAOImpl extends DAO implements ConceptDAO {

    private final ConceptNameDAO conceptNameDAO;

    @Inject
    public ConceptDAOImpl(EntityManager entityManager) {
        super(entityManager);
        this.conceptNameDAO = new ConceptNameDAOImpl(entityManager);
    }

    public Concept findRoot() {
        List<Concept> roots = findByNamedQuery("Concept.findRoot", new HashMap<String, Object>());
        if (roots.size() > 1) {
            throw new VARSPersistenceException("ERROR!! More than one root was found in the knowedgebase");
        }
        else if (roots.size() == 0) {
            throw new VARSPersistenceException("ERROR!! No root was found in the knowedgebase");
        }
        return roots.get(0);
    }

    /**
     * This find method should be called inside of a transaction
     * @param name
     * @return
     */
    public Concept findByName(String name) {
        ConceptName conceptName = conceptNameDAO.findByName(name);
        return conceptName == null ? null : merge(conceptName.getConcept());
    }

    /**
     * This should be called within a JPA tranaction
     * @param concept
     * @return
     */
    public Collection<ConceptName> findDescendentNames(Concept concept) {

        Collection<ConceptName> conceptNames = new ArrayList<ConceptName>();

        Concept mergedConcept = findByPrimaryKey(ConceptImpl.class, ((JPAEntity) concept).getId());
        conceptNames.addAll(mergedConcept.getConceptNames());
        findDescendentNames(mergedConcept.getChildConcepts(), conceptNames);

        return conceptNames;

    }


    /**
     * Private method for recursively collecting conceptnames
     * @param concepts A collection of concepts. Normally this is from concept.getChildConcepts()
     * @param conceptNames The colleciton of ConceptNames used to collect all the individual
     *      ConceptName objects
     */
    private void findDescendentNames(Collection<Concept> concepts, Collection<ConceptName> conceptNames) {
        for (Concept concept : concepts) {
            conceptNames.addAll(concept.getConceptNames());
            findDescendentNames(concept.getChildConcepts(), conceptNames);
        }
    }


    public Collection<Concept> findAll() {
        Map<String, Object> params = new HashMap<String, Object>();
        return findByNamedQuery("Concept.findAll", params);
    }

    /**
     * Should be called within a JPA transaction
     * @param concept
     * @return
     */
    public Collection<Concept> findDescendents(Concept concept) {
        Collection<Concept> concepts = new ArrayList<Concept>();
        Concept mergedConcept = findByPrimaryKey(ConceptImpl.class, ((JPAEntity) concept).getId());
        findDescendents(mergedConcept, concepts);

        return concepts;
    }

    private void findDescendents(Concept concept, Collection<Concept> concepts) {
        concepts.add(concept);
        for (Concept child : concept.getChildConcepts()) {
            findDescendents(child, concepts);
        }
    }



}
