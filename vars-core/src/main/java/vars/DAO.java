/*
 * @(#)DAO.java   2009.11.30 at 11:49:51 PST
 *
 * Copyright 2009 MBARI
 *
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */



package vars;

import java.util.List;
import java.util.Map;
import javax.persistence.EntityManager;

/**
 *
 * @author brian
 */
public interface DAO {

    void commit();

    void endTransaction();

    /**
     * True if the to objects represent the same object in the datastore. (e.g.
     * It basically compares the primary key)
     * @return
     */
    boolean equalInDatastore(Object thisObj, Object thatObj);

    /**
     * Executes a named query using a map of named parameters
     *
     * @param name
     *            The name of the query to execute
     * @param namedParameters
     *            A Map<String, Object> of the 'named' parameters to assign in
     *            the query
     * @param endTransaction if true the transaction wll be ended when the method exits. If
     *     false then the transaction will be kept open and can be reused by the current thread.
     * @return A list of objects returned by the query.
     */
    List findByNamedQuery(String name, Map<String, Object> namedParameters);

    <T> T findByPrimaryKey(Class<T> clazz, Object primaryKey);

    <T> T findInDatastore(T object);

    EntityManager getEntityManager();

    /**
     * Many one-to-many relations are lazy loaded in JPA. For convenience, this
     * method will load all lazy relations of an IEntity object. This method has
     * no effect on objects that are not persistent
     *
     * @param entity
     *            The persistent object who's children will be loaded from the
     *            database.
     */
    void loadLazyRelations(Object entity);

    <T> T merge(T object);

    /**
     *
     * @param object
     */
    void persist(Object object);

    /**
     *
     * @param object
     */
    void remove(Object object);

    void startTransaction();
}
