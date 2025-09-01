//package com.github.silent.samurai.speedy.jpa.impl.processors;
//
//import com.github.silent.samurai.speedy.interfaces.ISpeedyConfiguration;
//import com.github.silent.samurai.speedy.interfaces.MetaModel;
//import com.github.silent.samurai.speedy.interfaces.MetaModelProcessor;
//import com.github.silent.samurai.speedy.metadata.MetaModelBuilder;
//import com.github.silent.samurai.speedy.metadata.MetadataBuilder;
//import jakarta.persistence.EntityManagerFactory;
//
//public class JpaMetaModelProcessor implements MetaModelProcessor {
//
//    private final JpaMetaModel jpaMetaModel;
//
//    public JpaMetaModelProcessor(ISpeedyConfiguration configuration, EntityManagerFactory entityManagerFactory) {
//        jpaMetaModel = new JpaMetaModel(configuration, entityManagerFactory);
//    }
//
//    @Override
//    public MetaModel getMetaModel() {
//        return jpaMetaModel;
//    }
//
//    @Override
//    public void processMetaModel(MetaModelBuilder builder) {
//        jpaMetaModel.process();
//    }
//}
