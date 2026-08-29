package com.DBArena.services.execution.validation;

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.ExpressionVisitorAdapter;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.statement.select.FromItem;
import net.sf.jsqlparser.statement.select.Join;
import net.sf.jsqlparser.statement.select.OrderByElement;
import net.sf.jsqlparser.statement.select.ParenthesedSelect;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectItem;
import net.sf.jsqlparser.statement.select.SelectVisitorAdapter;
import net.sf.jsqlparser.statement.select.SetOperationList;
import net.sf.jsqlparser.statement.select.WithItem;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Every function name called anywhere in a statement - select list, WHERE,
 * HAVING, GROUP BY, ORDER BY, join ON-conditions, and any nested subquery
 * (FROM, WHERE, or a CTE), however deep.
 *
 * <p>An earlier version of this class piggy-backed on
 * {@link net.sf.jsqlparser.util.TablesNamesFinder}'s own traversal (it
 * already implements every visitor interface, so overriding just
 * {@code visit(Function)} looked like a free ride) - a real test case
 * ({@code "SELECT * FROM orders WHERE pg_sleep(5) IS NOT NULL"}) caught
 * that this misses function calls wrapped in some boolean-test expressions,
 * because {@code TablesNamesFinder} is purpose-built to find table
 * references and takes shortcuts wherever a table reference can't appear -
 * exactly the kind of shortcut that is fine for that job and unsafe for
 * this one. This version drives the clause-by-clause walk explicitly
 * itself (never assuming another visitor's traversal happens to cover what
 * this needs) and only relies on {@link ExpressionVisitorAdapter} - built
 * specifically for exhaustive expression-tree visitation, not a
 * task-specific shortcut-taking tool - for recursing *within* one
 * expression tree (operands, function parameters, CASE branches, etc).
 */
final class FunctionCallCollector {

    private final Set<String> functionNames = new LinkedHashSet<>();
    private final ExpressionVisitorAdapter expressionVisitor = new FunctionRecordingExpressionVisitor();
    private final SelectVisitorAdapter selectVisitor = new ClauseWalkingSelectVisitor();

    Set<String> collect(Select select) {
        functionNames.clear();
        expressionVisitor.setSelectVisitor(selectVisitor);
        select.accept(selectVisitor);
        return Set.copyOf(functionNames);
    }

    private void acceptIfPresent(Expression expression) {
        if (expression != null) {
            expression.accept(expressionVisitor);
        }
    }

    private void visitIfSubquery(FromItem fromItem) {
        if (fromItem instanceof ParenthesedSelect parenthesedSelect) {
            parenthesedSelect.getSelect().accept(selectVisitor);
        }
    }

    private final class FunctionRecordingExpressionVisitor extends ExpressionVisitorAdapter {
        @Override
        public void visit(Function function) {
            List<String> parts = function.getMultipartName();
            String simpleName = (parts == null || parts.isEmpty()) ? function.getName() : parts.get(parts.size() - 1);
            if (simpleName != null) {
                functionNames.add(simpleName.toLowerCase(Locale.ROOT));
            }
            super.visit(function); // keeps recursing into this function's own parameters
        }
    }

    private final class ClauseWalkingSelectVisitor extends SelectVisitorAdapter {
        @Override
        public void visit(PlainSelect plainSelect) {
            if (plainSelect.getSelectItems() != null) {
                for (SelectItem<?> item : plainSelect.getSelectItems()) {
                    acceptIfPresent(item.getExpression());
                }
            }
            acceptIfPresent(plainSelect.getWhere());
            acceptIfPresent(plainSelect.getHaving());
            if (plainSelect.getGroupBy() != null) {
                acceptIfPresent(plainSelect.getGroupBy().getGroupByExpressionList());
            }
            if (plainSelect.getOrderByElements() != null) {
                for (OrderByElement element : plainSelect.getOrderByElements()) {
                    acceptIfPresent(element.getExpression());
                }
            }
            visitIfSubquery(plainSelect.getFromItem());
            if (plainSelect.getJoins() != null) {
                for (Join join : plainSelect.getJoins()) {
                    visitIfSubquery(join.getRightItem());
                    if (join.getOnExpressions() != null) {
                        join.getOnExpressions().forEach(FunctionCallCollector.this::acceptIfPresent);
                    }
                }
            }
            if (plainSelect.getWithItemsList() != null) {
                for (WithItem withItem : plainSelect.getWithItemsList()) {
                    withItem.getSelect().accept(this);
                }
            }
        }

        @Override
        public void visit(ParenthesedSelect parenthesedSelect) {
            parenthesedSelect.getSelect().accept(this);
        }

        @Override
        public void visit(SetOperationList setOperationList) {
            for (Select select : setOperationList.getSelects()) {
                select.accept(this);
            }
        }

        @Override
        public void visit(WithItem withItem) {
            withItem.getSelect().accept(this);
        }
    }
}
