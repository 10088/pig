/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.pig.experimental.logical.expression;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.apache.pig.FuncSpec;
import org.apache.pig.PigException;
import org.apache.pig.backend.hadoop.executionengine.physicalLayer.LogicalToPhysicalTranslatorException;
import org.apache.pig.backend.hadoop.executionengine.physicalLayer.PhysicalOperator;
import org.apache.pig.backend.hadoop.executionengine.physicalLayer.expressionOperators.BinaryComparisonOperator;
import org.apache.pig.backend.hadoop.executionengine.physicalLayer.expressionOperators.ConstantExpression;
import org.apache.pig.backend.hadoop.executionengine.physicalLayer.expressionOperators.EqualToExpr;
import org.apache.pig.backend.hadoop.executionengine.physicalLayer.expressionOperators.ExpressionOperator;
import org.apache.pig.backend.hadoop.executionengine.physicalLayer.expressionOperators.GreaterThanExpr;
import org.apache.pig.backend.hadoop.executionengine.physicalLayer.expressionOperators.LessThanExpr;
import org.apache.pig.backend.hadoop.executionengine.physicalLayer.expressionOperators.POAnd;
import org.apache.pig.backend.hadoop.executionengine.physicalLayer.expressionOperators.POCast;
import org.apache.pig.backend.hadoop.executionengine.physicalLayer.expressionOperators.POOr;
import org.apache.pig.backend.hadoop.executionengine.physicalLayer.expressionOperators.POProject;
import org.apache.pig.backend.hadoop.executionengine.physicalLayer.expressionOperators.PORelationToExprProject;
import org.apache.pig.backend.hadoop.executionengine.physicalLayer.plans.PhysicalPlan;
import org.apache.pig.data.DataType;
import org.apache.pig.experimental.logical.relational.LogicalRelationalOperator;
import org.apache.pig.experimental.plan.DependencyOrderWalker;
import org.apache.pig.experimental.plan.Operator;
import org.apache.pig.experimental.plan.OperatorPlan;
import org.apache.pig.experimental.plan.PlanWalker;
import org.apache.pig.impl.PigContext;
import org.apache.pig.impl.plan.NodeIdGenerator;
import org.apache.pig.impl.plan.OperatorKey;
import org.apache.pig.impl.plan.PlanException;

public class ExpToPhyTranslationVisitor extends LogicalExpressionVisitor {

    // This value points to the current LogicalRelationalOperator we are working on
    protected LogicalRelationalOperator currentOp;
    
    public ExpToPhyTranslationVisitor(OperatorPlan plan, LogicalRelationalOperator op, PhysicalPlan phyPlan, Map<Operator, PhysicalOperator> map) {
        super(plan, new DependencyOrderWalker(plan));
        currentOp = op;
        logToPhyMap = map;
        currentPlan = phyPlan;
        currentPlans = new Stack<PhysicalPlan>();
    }
    
    public ExpToPhyTranslationVisitor(OperatorPlan plan, PlanWalker walker, LogicalRelationalOperator op, PhysicalPlan phyPlan, Map<Operator, PhysicalOperator> map) {
        super(plan, walker);
        currentOp = op;
        logToPhyMap = map;
        currentPlan = phyPlan;
        currentPlans = new Stack<PhysicalPlan>();
    }
    
    protected Map<Operator, PhysicalOperator> logToPhyMap;

    protected Stack<PhysicalPlan> currentPlans;

    protected PhysicalPlan currentPlan;

    protected NodeIdGenerator nodeGen = NodeIdGenerator.getGenerator();

    protected PigContext pc;
    
    public void setPigContext(PigContext pc) {
        this.pc = pc;
    }

    public PhysicalPlan getPhysicalPlan() {
        return currentPlan;
    }

    @Override
    public void visitAnd( AndExpression op ) throws IOException {
        String scope = DEFAULT_SCOPE;
//        System.err.println("Entering And");
        BinaryComparisonOperator exprOp = new POAnd(new OperatorKey(scope, nodeGen.getNextNodeId(scope)));
        // We dont have aliases in ExpressionOperators
        // exprOp.setAlias(op.getAlias());
        exprOp.setLhs((ExpressionOperator)logToPhyMap.get(op.getLhs()));
        exprOp.setRhs((ExpressionOperator)logToPhyMap.get(op.getRhs()));
        OperatorPlan oPlan = op.getPlan();
        
        currentPlan.add(exprOp);
        logToPhyMap.put(op, exprOp);
        
        List<Operator> successors = oPlan.getSuccessors(op);
        if(successors == null) return;
        for(Operator lo : successors) {
            PhysicalOperator from = logToPhyMap.get(lo);
            try {
                currentPlan.connect(from, exprOp);
            } catch (PlanException e) {
                int errCode = 2015;
                String msg = "Invalid physical operators in the physical plan" ;
                throw new LogicalToPhysicalTranslatorException(msg, errCode, PigException.BUG, e);
            }
        }
//        System.err.println("Exiting And");
    }
    
    @Override
    public void visitOr( OrExpression op ) throws IOException {
        String scope = DEFAULT_SCOPE;
//        System.err.println("Entering Or");
        BinaryComparisonOperator exprOp = new POOr(new OperatorKey(scope, nodeGen.getNextNodeId(scope)));
        // We dont have aliases in ExpressionOperators
        // exprOp.setAlias(op.getAlias());
        exprOp.setLhs((ExpressionOperator)logToPhyMap.get(op.getLhs()));
        exprOp.setRhs((ExpressionOperator)logToPhyMap.get(op.getRhs()));
        OperatorPlan oPlan = op.getPlan();
        
        currentPlan.add(exprOp);
        logToPhyMap.put(op, exprOp);
        
        List<Operator> successors = oPlan.getSuccessors(op);
        if(successors == null) return;
        for(Operator lo : successors) {
            PhysicalOperator from = logToPhyMap.get(lo);
            try {
                currentPlan.connect(from, exprOp);
            } catch (PlanException e) {
                int errCode = 2015;
                String msg = "Invalid physical operators in the physical plan" ;
                throw new LogicalToPhysicalTranslatorException(msg, errCode, PigException.BUG, e);
            }
        }
//        System.err.println("Exiting Or");
    }
    
    @Override
    public void visitEqual( EqualExpression op ) throws IOException {
        String scope = DEFAULT_SCOPE;
        BinaryComparisonOperator exprOp = new EqualToExpr(new OperatorKey(
                scope, nodeGen.getNextNodeId(scope)));
        // We dont have aliases in ExpressionOperators
        // exprOp.setAlias(op.getAlias());
        exprOp.setOperandType(op.getLhs().getType());
        exprOp.setLhs((ExpressionOperator) logToPhyMap.get(op.getLhs()));
        exprOp.setRhs((ExpressionOperator) logToPhyMap.get(op.getRhs()));
        OperatorPlan oPlan = op.getPlan();
        
        currentPlan.add(exprOp);
        logToPhyMap.put(op, exprOp);

        List<Operator> successors = oPlan.getSuccessors(op);
        if (successors == null) {
            return;
        }
        for (Operator lo : successors) {
            PhysicalOperator from = logToPhyMap.get(lo);
            try {
                currentPlan.connect(from, exprOp);
            } catch (PlanException e) {
                int errCode = 2015;
                String msg = "Invalid physical operators in the physical plan" ;
                throw new LogicalToPhysicalTranslatorException(msg, errCode, PigException.BUG, e);
            }
        }
    }
    
    @Override
    public void visitGreaterThan( GreaterThanExpression op ) throws IOException {
        String scope = DEFAULT_SCOPE;
        BinaryComparisonOperator exprOp = new GreaterThanExpr(new OperatorKey(
                scope, nodeGen.getNextNodeId(scope)));
        // We dont have aliases in ExpressionOperators
        // exprOp.setAlias(op.getAlias());
        exprOp.setOperandType(op.getLhs().getType());
        exprOp.setLhs((ExpressionOperator) logToPhyMap.get(op.getLhs()));
        exprOp.setRhs((ExpressionOperator) logToPhyMap.get(op.getRhs()));
        OperatorPlan oPlan = op.getPlan();
        
        currentPlan.add(exprOp);
        logToPhyMap.put(op, exprOp);

        List<Operator> successors = oPlan.getSuccessors(op);
        if (successors == null) {
            return;
        }
        for (Operator lo : successors) {
            PhysicalOperator from = logToPhyMap.get(lo);
            try {
                currentPlan.connect(from, exprOp);
            } catch (PlanException e) {
                int errCode = 2015;
                String msg = "Invalid physical operators in the physical plan" ;
                throw new LogicalToPhysicalTranslatorException(msg, errCode, PigException.BUG, e);
            }
        }
    }
    
    @Override
    public void visitGreaterThanEqual( GreaterThanEqualExpression op ) throws IOException {
        String scope = DEFAULT_SCOPE;
        BinaryComparisonOperator exprOp = new LessThanExpr(new OperatorKey(
                scope, nodeGen.getNextNodeId(scope)));
        // We dont have aliases in ExpressionOperators
        // exprOp.setAlias(op.getAlias());
        exprOp.setOperandType(op.getLhs().getType());
        exprOp.setLhs((ExpressionOperator) logToPhyMap.get(op.getLhs()));
        exprOp.setRhs((ExpressionOperator) logToPhyMap.get(op.getRhs()));
        OperatorPlan oPlan = op.getPlan();
        
        currentPlan.add(exprOp);
        logToPhyMap.put(op, exprOp);

        List<Operator> successors = oPlan.getSuccessors(op);
        if (successors == null) {
            return;
        }
        for (Operator lo : successors) {
            PhysicalOperator from = logToPhyMap.get(lo);
            try {
                currentPlan.connect(from, exprOp);
            } catch (PlanException e) {
                int errCode = 2015;
                String msg = "Invalid physical operators in the physical plan" ;
                throw new LogicalToPhysicalTranslatorException(msg, errCode, PigException.BUG, e);
            }
        }
    }
    
    @Override
    public void visitLessThan( LessThanExpression op ) throws IOException {
        String scope = DEFAULT_SCOPE;
        BinaryComparisonOperator exprOp = new LessThanExpr(new OperatorKey(
                scope, nodeGen.getNextNodeId(scope)));
        // We dont have aliases in ExpressionOperators
        // exprOp.setAlias(op.getAlias());
        exprOp.setOperandType(op.getLhs().getType());
        exprOp.setLhs((ExpressionOperator) logToPhyMap.get(op.getLhs()));
        exprOp.setRhs((ExpressionOperator) logToPhyMap.get(op.getRhs()));
        OperatorPlan oPlan = op.getPlan();
        
        currentPlan.add(exprOp);
        logToPhyMap.put(op, exprOp);

        List<Operator> successors = oPlan.getSuccessors(op);
        if (successors == null) {
            return;
        }
        for (Operator lo : successors) {
            PhysicalOperator from = logToPhyMap.get(lo);
            try {
                currentPlan.connect(from, exprOp);
            } catch (PlanException e) {
                int errCode = 2015;
                String msg = "Invalid physical operators in the physical plan" ;
                throw new LogicalToPhysicalTranslatorException(msg, errCode, PigException.BUG, e);
            }
        }
    }
    
    
    @Override
    public void visitLessThanEqual( LessThanEqualExpression op ) throws IOException {
        String scope = DEFAULT_SCOPE;
        BinaryComparisonOperator exprOp = new LessThanExpr(new OperatorKey(
                scope, nodeGen.getNextNodeId(scope)));
        // We dont have aliases in ExpressionOperators
        // exprOp.setAlias(op.getAlias());
        exprOp.setOperandType(op.getLhs().getType());
        exprOp.setLhs((ExpressionOperator) logToPhyMap.get(op.getLhs()));
        exprOp.setRhs((ExpressionOperator) logToPhyMap.get(op.getRhs()));
        OperatorPlan oPlan = op.getPlan();
        
        currentPlan.add(exprOp);
        logToPhyMap.put(op, exprOp);

        List<Operator> successors = oPlan.getSuccessors(op);
        if (successors == null) {
            return;
        }
        for (Operator lo : successors) {
            PhysicalOperator from = logToPhyMap.get(lo);
            try {
                currentPlan.connect(from, exprOp);
            } catch (PlanException e) {
                int errCode = 2015;
                String msg = "Invalid physical operators in the physical plan" ;
                throw new LogicalToPhysicalTranslatorException(msg, errCode, PigException.BUG, e);
            }
        }
    }
    
    @Override
    public void visitProject(ProjectExpression op) throws IOException {
        String scope = DEFAULT_SCOPE;
//        System.err.println("Entering Project");
        POProject exprOp;
       
        if(op.getType() == DataType.BAG) {
            exprOp = new PORelationToExprProject(new OperatorKey(scope, nodeGen
                .getNextNodeId(scope)));
         } else {
            exprOp = new POProject(new OperatorKey(scope, nodeGen
                .getNextNodeId(scope)));
        }
        // We dont have aliases in ExpressionOperators
        // exprOp.setAlias(op.getAlias());
        exprOp.setResultType(op.getType());
        exprOp.setColumn(op.getColNum());
        // TODO implement this
//        exprOp.setStar(op.isStar());
//        exprOp.setOverloaded(op.getOverloaded());
        logToPhyMap.put(op, exprOp);
        currentPlan.add(exprOp);
        
        // We only have one input so connection is required from only one predecessor
//        PhysicalOperator from = logToPhyMap.get(op.findReferent(currentOp));
//        currentPlan.connect(from, exprOp);
        
//        List<Operator> predecessors = lp.getPredecessors(op);
//
//        // Project might not have any predecessors
//        if (predecessors == null)
//            return;
//
//        for (Operator lo : predecessors) {
//            PhysicalOperator from = logToPhyMap.get(lo);
//            try {
//                currentPlan.connect(from, exprOp);
//            } catch (PlanException e) {
//                int errCode = 2015;
//                String msg = "Invalid physical operators in the physical plan" ;
//                throw new LogicalToPhysicalTranslatorException(msg, errCode, PigException.BUG, e);
//            }
//        }
//        System.err.println("Exiting Project");
    }
    
    @Override
    public void visitConstant(org.apache.pig.experimental.logical.expression.ConstantExpression op) throws IOException {
        String scope = DEFAULT_SCOPE;
//        System.err.println("Entering Constant");
        ConstantExpression ce = new ConstantExpression(new OperatorKey(scope,
                nodeGen.getNextNodeId(scope)));
        // We dont have aliases in ExpressionOperators
        // ce.setAlias(op.getAlias());
        ce.setValue(op.getValue());
        ce.setResultType(op.getType());
        //this operator doesn't have any predecessors
        currentPlan.add(ce);
        logToPhyMap.put(op, ce);
//        System.err.println("Exiting Constant");
    }
    
    @Override
    public void visitCast( CastExpression op ) throws IOException {
        String scope = DEFAULT_SCOPE;
        POCast pCast = new POCast(new OperatorKey(scope, nodeGen
                .getNextNodeId(scope)));
//        physOp.setAlias(op.getAlias());
        currentPlan.add(pCast);

        logToPhyMap.put(op, pCast);
        ExpressionOperator from = (ExpressionOperator) logToPhyMap.get(op
                .getExpression());
        pCast.setResultType(op.getType());
        FuncSpec lfSpec = op.getFuncSpec();
        if(null != lfSpec) {
            pCast.setFuncSpec(lfSpec);
        }
        try {
            currentPlan.connect(from, pCast);
        } catch (PlanException e) {
            int errCode = 2015;
            String msg = "Invalid physical operators in the physical plan" ;
            throw new LogicalToPhysicalTranslatorException(msg, errCode, PigException.BUG, e);
        }
    }
}
