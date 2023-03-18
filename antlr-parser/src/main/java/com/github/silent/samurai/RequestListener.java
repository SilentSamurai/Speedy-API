package com.github.silent.samurai;




import java.util.ArrayList;
import java.util.List;

public class RequestListener extends SpeedyBaseListener {

    private List<Request> requests = new ArrayList<>();
    private Request current;

    @Override
    public void enterRequest(SpeedyParser.RequestContext ctx) {
        current = new Request();
    }

    @Override
    public void exitRequest(SpeedyParser.RequestContext ctx) {
        requests.add(current);
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
    public void enterKeywordsParams(SpeedyParser.KeywordsParamsContext ctx) {
        current.getKeywords().put(ctx.paramKey().identifier().getText(), ctx.paramValue().valString().getText());
    }

    @Override
    public void enterSearchParameter(SpeedyParser.SearchParameterContext ctx) {
        current.getQuery().add(ctx.identifier().getText(), ctx.valString().getText());
    }

    public List<Request> getEntries() {
        return requests;
    }
}
