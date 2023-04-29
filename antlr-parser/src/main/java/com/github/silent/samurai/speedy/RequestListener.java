package com.github.silent.samurai.speedy;


import com.github.silent.samurai.SpeedyBaseListener;
import com.github.silent.samurai.SpeedyParser;
import com.github.silent.samurai.speedy.models.AntlrRequest;
import com.github.silent.samurai.speedy.models.Filter;
import com.github.silent.samurai.speedy.models.Query;
import org.antlr.v4.runtime.RuleContext;

import java.util.ArrayList;
import java.util.List;

public class RequestListener extends SpeedyBaseListener {

    private final List<AntlrRequest> antlrRequests = new ArrayList<>();
    private AntlrRequest current;
    private Filter currentFilter;
    private Query currentQuery;

    @Override
    public void enterRequest(SpeedyParser.RequestContext ctx) {
        current = new AntlrRequest();
    }

    @Override
    public void exitRequest(SpeedyParser.RequestContext ctx) {
        antlrRequests.add(current);
    }

    @Override
    public void enterResource(SpeedyParser.ResourceContext ctx) {
        current.setResource(ctx.getText());
    }

    @Override
    public void enterArgument(SpeedyParser.ArgumentContext ctx) {
        current.getArguments().add(ctx.getText());
    }

    @Override
    public void enterKeywordsParams(SpeedyParser.KeywordsParamsContext ctx) {
        currentFilter = new Filter();
        currentFilter.setField(ctx.identifier().getText());
    }

    @Override
    public void enterParamSV(SpeedyParser.ParamSVContext ctx) {
        currentFilter.setOperator(ctx.svoptr().getText());
        currentFilter.addValue(ctx.constValue().getText());
    }

    @Override
    public void enterParamMV(SpeedyParser.ParamMVContext ctx) {
        currentFilter.setOperator(ctx.mvoptr().getText());
        ctx.constList().constValue().stream()
                .map(RuleContext::getText)
                .forEach(currentFilter::addValue);
    }

    @Override
    public void exitKeywordsParams(SpeedyParser.KeywordsParamsContext ctx) {
        current.addFilter(currentFilter);
    }

    @Override
    public void enterCndoptr(SpeedyParser.CndoptrContext ctx) {
        current.getFilterOrder().add(ctx.getText());
    }


    @Override
    public void enterSearchParameter(SpeedyParser.SearchParameterContext ctx) {
        currentQuery = new Query();
        currentQuery.setIdentifier(ctx.identifier().getText());
    }

    @Override
    public void enterSearchSV(SpeedyParser.SearchSVContext ctx) {
        currentQuery.addValue(ctx.constValue().getText());
    }

    @Override
    public void enterSearchMV(SpeedyParser.SearchMVContext ctx) {
        ctx.constList().constValue().stream()
                .map(RuleContext::getText)
                .forEach(currentQuery::addValue);
    }

    @Override
    public void exitSearchParameter(SpeedyParser.SearchParameterContext ctx) {
        current.getQueries().add(ctx.identifier().getText(), currentQuery);
    }


    public List<AntlrRequest> getEntries() {
        return antlrRequests;
    }
}
