/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.envers.test.integration.manytomany.ternary;

import java.util.Arrays;
import javax.persistence.EntityManager;

import org.junit.Assert;
import org.junit.Test;

import org.hibernate.ejb.Ejb3Configuration;
import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;
import org.hibernate.envers.test.entities.IntTestPrivSeqEntity;
import org.hibernate.envers.test.entities.StrTestPrivSeqEntity;
import org.hibernate.envers.test.tools.TestTools;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class TernaryMap extends BaseEnversJPAFunctionalTestCase {
    private Integer str1_id;
    private Integer str2_id;

    private Integer int1_id;
    private Integer int2_id;

    private Integer map1_id;
    private Integer map2_id;

    public void configure(Ejb3Configuration cfg) {
        cfg.addAnnotatedClass(TernaryMapEntity.class);
        cfg.addAnnotatedClass(StrTestPrivSeqEntity.class);
        cfg.addAnnotatedClass(IntTestPrivSeqEntity.class);
    }

    @Test
    @Priority(10)
    public void initData() {
        EntityManager em = getEntityManager();

        StrTestPrivSeqEntity str1 = new StrTestPrivSeqEntity("a");
        StrTestPrivSeqEntity str2 = new StrTestPrivSeqEntity("b");

        IntTestPrivSeqEntity int1 = new IntTestPrivSeqEntity(1);
        IntTestPrivSeqEntity int2 = new IntTestPrivSeqEntity(2);

        TernaryMapEntity map1 = new TernaryMapEntity();
        TernaryMapEntity map2 = new TernaryMapEntity();

        // Revision 1 (map1: initialy one mapping int1 -> str1, map2: empty)
        em.getTransaction().begin();

        em.persist(str1);
        em.persist(str2);
        em.persist(int1);
        em.persist(int2);

        map1.getMap().put(int1, str1);

        em.persist(map1);
        em.persist(map2);

        em.getTransaction().commit();

        // Revision 2 (map1: replacing the mapping, map2: adding two mappings)

        em.getTransaction().begin();

        map1 = em.find(TernaryMapEntity.class, map1.getId());
        map2 = em.find(TernaryMapEntity.class, map2.getId());

        str1 = em.find(StrTestPrivSeqEntity.class, str1.getId());
        str2 = em.find(StrTestPrivSeqEntity.class, str2.getId());

        int1 = em.find(IntTestPrivSeqEntity.class, int1.getId());
        int2 = em.find(IntTestPrivSeqEntity.class, int2.getId());

        map1.getMap().put(int1, str2);

        map2.getMap().put(int1, str1);
        map2.getMap().put(int2, str1);

        em.getTransaction().commit();

        // Revision 3 (map1: removing a non-existing mapping, adding an existing mapping - no changes, map2: removing a mapping)
        em.getTransaction().begin();

        map1 = em.find(TernaryMapEntity.class, map1.getId());
        map2 = em.find(TernaryMapEntity.class, map2.getId());

        str2 = em.find(StrTestPrivSeqEntity.class, str2.getId());

        int1 = em.find(IntTestPrivSeqEntity.class, int1.getId());
        int2 = em.find(IntTestPrivSeqEntity.class, int2.getId());

        map1.getMap().remove(int2);
        map1.getMap().put(int1, str2);

        map2.getMap().remove(int1);

        em.getTransaction().commit();

        // Revision 4 (map1: adding a mapping, map2: adding a mapping)
        em.getTransaction().begin();

        map1 = em.find(TernaryMapEntity.class, map1.getId());
        map2 = em.find(TernaryMapEntity.class, map2.getId());

        str2 = em.find(StrTestPrivSeqEntity.class, str2.getId());

        int1 = em.find(IntTestPrivSeqEntity.class, int1.getId());
        int2 = em.find(IntTestPrivSeqEntity.class, int2.getId());

        map1.getMap().put(int2, str2);

        map2.getMap().put(int1, str2);

        em.getTransaction().commit();
        //

        map1_id = map1.getId();
        map2_id = map2.getId();

        str1_id = str1.getId();
        str2_id = str2.getId();

        int1_id = int1.getId();
        int2_id = int2.getId();
    }

    @Test
    public void testRevisionsCounts() {
        assert Arrays.asList(1, 2, 4).equals(getAuditReader().getRevisions(TernaryMapEntity.class, map1_id));
        assert Arrays.asList(1, 2, 3, 4).equals(getAuditReader().getRevisions(TernaryMapEntity.class, map2_id));

        assert Arrays.asList(1).equals(getAuditReader().getRevisions(StrTestPrivSeqEntity.class, str1_id));
        assert Arrays.asList(1).equals(getAuditReader().getRevisions(StrTestPrivSeqEntity.class, str2_id));

        assert Arrays.asList(1).equals(getAuditReader().getRevisions(IntTestPrivSeqEntity.class, int1_id));
        assert Arrays.asList(1).equals(getAuditReader().getRevisions(IntTestPrivSeqEntity.class, int2_id));
    }

    @Test
    public void testHistoryOfMap1() {
        StrTestPrivSeqEntity str1 = getEntityManager().find(StrTestPrivSeqEntity.class, str1_id);
        StrTestPrivSeqEntity str2 = getEntityManager().find(StrTestPrivSeqEntity.class, str2_id);

        IntTestPrivSeqEntity int1 = getEntityManager().find(IntTestPrivSeqEntity.class, int1_id);
        IntTestPrivSeqEntity int2 = getEntityManager().find(IntTestPrivSeqEntity.class, int2_id);

        TernaryMapEntity rev1 = getAuditReader().find(TernaryMapEntity.class, map1_id, 1);
        TernaryMapEntity rev2 = getAuditReader().find(TernaryMapEntity.class, map1_id, 2);
        TernaryMapEntity rev3 = getAuditReader().find(TernaryMapEntity.class, map1_id, 3);
        TernaryMapEntity rev4 = getAuditReader().find(TernaryMapEntity.class, map1_id, 4);

        Assert.assertEquals(TestTools.makeMap(int1, str1), rev1.getMap());
        Assert.assertEquals(TestTools.makeMap(int1, str2), rev2.getMap());
        Assert.assertEquals(TestTools.makeMap(int1, str2), rev3.getMap());
        Assert.assertEquals(TestTools.makeMap(int1, str2, int2, str2), rev4.getMap());
    }

    @Test
    public void testHistoryOfMap2() {
        StrTestPrivSeqEntity str1 = getEntityManager().find(StrTestPrivSeqEntity.class, str1_id);
        StrTestPrivSeqEntity str2 = getEntityManager().find(StrTestPrivSeqEntity.class, str2_id);

        IntTestPrivSeqEntity int1 = getEntityManager().find(IntTestPrivSeqEntity.class, int1_id);
        IntTestPrivSeqEntity int2 = getEntityManager().find(IntTestPrivSeqEntity.class, int2_id);

        TernaryMapEntity rev1 = getAuditReader().find(TernaryMapEntity.class, map2_id, 1);
        TernaryMapEntity rev2 = getAuditReader().find(TernaryMapEntity.class, map2_id, 2);
        TernaryMapEntity rev3 = getAuditReader().find(TernaryMapEntity.class, map2_id, 3);
        TernaryMapEntity rev4 = getAuditReader().find(TernaryMapEntity.class, map2_id, 4);

        assert rev1.getMap().equals(TestTools.makeMap());
        assert rev2.getMap().equals(TestTools.makeMap(int1, str1, int2, str1));
        assert rev3.getMap().equals(TestTools.makeMap(int2, str1));
        assert rev4.getMap().equals(TestTools.makeMap(int1, str2, int2, str1));
    }
}