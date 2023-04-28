package com.github.silent.samurai;


import com.github.silent.samurai.speedy.model.AntlrRequest;
import com.github.silent.samurai.speedy.model.Filter;
import com.github.silent.samurai.speedy.model.FilterValue;

import java.util.ArrayList;
import java.util.List;

public class RequestListener extends SpeedyBaseListener {

    private final List<AntlrRequest> antlrRequests = new ArrayList<>();
    private AntlrRequest current;
    private Filter currentFilter;
    private FilterValue currentValue;

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
        current.getArguments().add(ctx.valString().getText());
    }

    @Override
    public void enterOperator(SpeedyParser.OperatorContext ctx) {
        currentFilter.setOperator(ctx.getText());
    }

    @Override
    public void enterParamKey(SpeedyParser.ParamKeyContext ctx) {
        currentFilter.setIdentifier(ctx.getText());
    }

    @Override
    public void enterConstValue(SpeedyParser.ConstValueContext ctx) {
        currentValue.addValue(ctx.getText());
    }

    @Override
    public void enterKeywordsParams(SpeedyParser.KeywordsParamsContext ctx) {
        currentFilter = new Filter();
        currentValue = new FilterValue();
        currentFilter.setValue(currentValue);
    }

    @Override
    public void exitKeywordsParams(SpeedyParser.KeywordsParamsContext ctx) {
        current.getKeywords().add(currentFilter.getIdentifier(), currentFilter);
    }

    @Override
    public void enterSearchParameter(SpeedyParser.SearchParameterContext ctx) {
        currentValue = new FilterValue();
        if (ctx.paramValue() != null) {
            current.getQuery().add(ctx.identifier().getText(), currentValue);
        } else {
            current.getQuery().add(ctx.identifier().getText(), currentValue);
        }
    }

    public List<AntlrRequest> getEntries() {
        return antlrRequests;
    }
}
