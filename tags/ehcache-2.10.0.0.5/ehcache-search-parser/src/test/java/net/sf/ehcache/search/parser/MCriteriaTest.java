/**
 *  Copyright Terracotta, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package net.sf.ehcache.search.parser;

import net.sf.ehcache.search.expression.And;
import net.sf.ehcache.search.expression.EqualTo;
import net.sf.ehcache.search.expression.GreaterThan;
import net.sf.ehcache.search.expression.GreaterThanOrEqual;
import net.sf.ehcache.search.expression.ILike;
import net.sf.ehcache.search.expression.LessThan;
import net.sf.ehcache.search.expression.LessThanOrEqual;
import net.sf.ehcache.search.expression.Not;
import net.sf.ehcache.search.expression.NotEqualTo;
import net.sf.ehcache.search.expression.Or;
import net.sf.ehcache.search.parser.MCriteria.Between;
import net.sf.ehcache.search.parser.MCriteria.Simple;
import net.sf.ehcache.search.parser.MValue.MInt;

import org.junit.Assert;
import org.junit.Test;

public class MCriteriaTest {

    private final ClassLoader loader = getClass().getClassLoader();

	@Test
    public void testSimpleModelCriteria() throws CustomParseException {
        {
            MInt obj = new MValue.MInt(null, "0");
            Simple mcrit = new MCriteria.Simple(new MAttribute("foo"), MCriteria.SimpleOp.EQ, obj);
            EqualTo crit = (EqualTo)mcrit.asEhcacheObject(loader);
            Assert.assertEquals(crit.getAttributeName(), "foo");
            Assert.assertEquals(crit.getValue(), obj.asEhcacheObject(loader));
        }
        {
            MInt obj = new MValue.MInt(null, "0");
            Simple mcrit = new MCriteria.Simple(new MAttribute("foo"), MCriteria.SimpleOp.NE, obj);
            NotEqualTo crit = (NotEqualTo)mcrit.asEhcacheObject(loader);
            Assert.assertEquals(crit.getAttributeName(), "foo");
            Assert.assertEquals(crit.getValue(), obj.asEhcacheObject(loader));
        }
        {
            MInt obj = new MValue.MInt(null, "0");
            Simple mcrit = new MCriteria.Simple(new MAttribute("foo"), MCriteria.SimpleOp.GE, obj);
            GreaterThanOrEqual crit = (GreaterThanOrEqual)mcrit.asEhcacheObject(loader);
            Assert.assertEquals(crit.getAttributeName(), "foo");
            Assert.assertEquals(crit.getComparableValue(), obj.asEhcacheObject(loader));
        }
        {
            MInt obj = new MValue.MInt(null, "0");
            Simple mcrit = new MCriteria.Simple(new MAttribute("foo"), MCriteria.SimpleOp.GT, obj);
            GreaterThan crit = (GreaterThan)mcrit.asEhcacheObject(loader);
            Assert.assertEquals(crit.getAttributeName(), "foo");
            Assert.assertEquals(crit.getComparableValue(), obj.asEhcacheObject(loader));
        }
        {
            MInt obj = new MValue.MInt(null, "0");
            Simple mcrit = new MCriteria.Simple(new MAttribute("foo"), MCriteria.SimpleOp.LT, obj);
            LessThan crit = (LessThan)mcrit.asEhcacheObject(loader);
            Assert.assertEquals(crit.getAttributeName(), "foo");
            Assert.assertEquals(crit.getComparableValue(), obj.asEhcacheObject(loader));
        }
        {
            MInt obj = new MValue.MInt(null, "0");
            Simple mcrit = new MCriteria.Simple(new MAttribute("foo"), MCriteria.SimpleOp.LE, obj);
            LessThanOrEqual crit = (LessThanOrEqual)mcrit.asEhcacheObject(loader);
            Assert.assertEquals(crit.getAttributeName(), "foo");
            Assert.assertEquals(crit.getComparableValue(), obj.asEhcacheObject(loader));
        }
    }

    void checkBetween(String name, ModelElement<?> obj1, boolean min, ModelElement<?> obj2, boolean max) {
        Between between = new MCriteria.Between(new MAttribute(name), obj1, min, obj2, max);
        net.sf.ehcache.search.expression.Between crit = (net.sf.ehcache.search.expression.Between)between.asEhcacheObject(loader);
        Assert.assertEquals(crit.getAttributeName(), name);
        Assert.assertEquals(crit.getMin(), obj1.asEhcacheObject(loader));
        Assert.assertEquals(crit.getMax(), obj2.asEhcacheObject(loader));
        Assert.assertEquals(crit.isMinInclusive(), min);
        Assert.assertEquals(crit.isMaxInclusive(), max);
    }

    @Test
    public void testBetween() throws CustomParseException {
        MInt obj1 = new MValue.MInt(null, "0");
        MInt obj2 = new MValue.MInt(null, "110");
        checkBetween("foo", obj1, false, obj2, false);
        checkBetween("foo", obj1, true, obj2, false);
        checkBetween("foo", obj1, false, obj2, true);
        checkBetween("foo", obj1, true, obj2, true);
    }

    @Test
    public void testIlike() {
        MCriteria.ILike ilike = new MCriteria.ILike(MAttribute.KEY, "foo.*foo");
        ILike crit = (ILike)ilike.asEhcacheObject(loader);
        Assert.assertEquals(crit.getRegex(), "foo.*foo");
        Assert.assertEquals(crit.getAttributeName(), MAttribute.KEY.getName());
    }

    @Test
    public void testLike() {
        MCriteria.Like like = new MCriteria.Like(MAttribute.KEY, "foo.%foo");
        ILike crit = (ILike)like.asEhcacheObject(loader);
        Assert.assertEquals(like.getLikeRegex(), "foo.%foo");
        Assert.assertEquals(like.getILikeRegex(), "foo.*foo");
        Assert.assertEquals(crit.getRegex(), "foo.*foo");
        Assert.assertEquals(crit.getAttributeName(), MAttribute.KEY.getName());
    }

    @Test
    public void testNot() throws CustomParseException {
        MCriteria.Simple simple = new MCriteria.Simple(MAttribute.KEY, MCriteria.SimpleOp.EQ, new MValue.MInt(null, "1"));
        MCriteria.Not not = new MCriteria.Not(simple);
        Not crit = (Not)not.asEhcacheObject(loader);
        @SuppressWarnings("unused")
        EqualTo inner = (EqualTo)crit.getCriteria();
        Assert.assertTrue(true); // the casts are sufficient for the test.
    }

    @Test
    public void testAnd() throws CustomParseException {
        Simple mcrit1 = new MCriteria.Simple(new MAttribute("foo"), MCriteria.SimpleOp.GT, new MValue.MInt(null, "0"));
        Simple mcrit2 = new MCriteria.Simple(new MAttribute("foo"), MCriteria.SimpleOp.LT, new MValue.MInt(null, "10"));
        Simple mcrit3 = new MCriteria.Simple(new MAttribute("foo"), MCriteria.SimpleOp.NE, new MValue.MInt(null, "5"));

        MCriteria.And and = new MCriteria.And(mcrit1, mcrit2, mcrit3);
        And crit = (And)and.asEhcacheObject(loader);
        Assert.assertTrue(crit.getCriterion()[0] instanceof GreaterThan);
        Assert.assertTrue(crit.getCriterion()[1] instanceof And);
        And crit2 = (And)crit.getCriterion()[1];
        Assert.assertTrue(crit2.getCriterion()[0] instanceof LessThan);
        Assert.assertTrue(crit2.getCriterion()[1] instanceof NotEqualTo);

    }

    @Test
    public void testOr() throws CustomParseException {
        Simple mcrit1 = new MCriteria.Simple(new MAttribute("foo"), MCriteria.SimpleOp.GT, new MValue.MInt(null, "100"));
        Simple mcrit2 = new MCriteria.Simple(new MAttribute("foo"), MCriteria.SimpleOp.LT, new MValue.MInt(null, "10"));
        MCriteria.Or or = new MCriteria.Or(mcrit1, mcrit2);

        Or crit = (Or)or.asEhcacheObject(loader);
        Assert.assertTrue(crit.getCriterion()[0] instanceof GreaterThan);
        Assert.assertTrue(crit.getCriterion()[1] instanceof LessThan);
    }

}
