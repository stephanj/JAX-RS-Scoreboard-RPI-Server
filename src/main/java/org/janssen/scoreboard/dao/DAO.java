/*
 *     Licensed to the Apache Software Foundation (ASF) under one or more
 *     contributor license agreements.  See the NOTICE file distributed with
 *     this work for additional information regarding copyright ownership.
 *     The ASF licenses this file to You under the Apache License, Version 2.0
 *     (the "License"); you may not use this file except in compliance with
 *     the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 */
package org.janssen.scoreboard.dao;

import org.janssen.scoreboard.model.Token;

import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import java.util.Date;
import java.util.List;

/**
 * Simply maps the entity manager.
 * It simplifies refactoring (unitName change) and wraps some logic (limited queries).
 *
 */
@Singleton
@Lock(LockType.READ)
public class DAO {

    @PersistenceContext(unitName = "scoreboard")
    private EntityManager em;

    public <E> E create(E e) {
        em.persist(e);
        return e;
    }

    public <E> E update(E e) {
        return em.merge(e);
    }

    public <E> void delete(Class<E> clazz, long id) {
        em.remove(em.find(clazz, id));
    }

    public <E> E find(Class<E> clazz, long id) {
        return em.find(clazz, id);
    }

    @SuppressWarnings("unchecked")
    public Object count(String query) {
        return em.createNamedQuery(query).getSingleResult();
    }

    @SuppressWarnings("unchecked")
    public <E> List<E> find(Class<E> clazz, String query, int min, int max) {
        return queryRange(em.createQuery(query, clazz), min, max).getResultList();
    }

    @SuppressWarnings("unchecked")
    public <E> List<E> queryFindByCreation(Class<E> clazz, String query, Date createdOn, int min, int max) {
        final TypedQuery<E> namedQuery = em.createNamedQuery(query, clazz);
        namedQuery.setParameter("oneDayOld", createdOn);
        return queryRange(namedQuery, min, max).getResultList();
    }

    @SuppressWarnings("unchecked")
    public <E> List<E> queryFindByToken(String query, String token) {
        Query namedQuery = em.createNamedQuery(query);
        namedQuery.setParameter("tokenValue", token);
        return namedQuery.getResultList();
    }

    private static Query queryRange(Query query, int min, int max) {
        if (max >= 0) {
            query.setMaxResults(max);
        }
        if (min >= 0) {
            query.setFirstResult(min);
        }
        return query;
    }
}
