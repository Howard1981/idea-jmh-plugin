package ru.artyushov.jmhPlugin.configuration;

import com.intellij.execution.JavaExecutionUtil;
import com.intellij.execution.Location;
import com.intellij.execution.actions.ConfigurationContext;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.Ref;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;

import java.util.Iterator;

/**
 * User: nikart
 * Date: 15/07/14
 * Time: 23:30
 */
public class JmhClassConfigurationProducer extends JmhConfigurationProducer {

    @Override
    protected boolean setupConfigurationFromContext(JmhConfiguration configuration, ConfigurationContext context,
                                                    Ref<PsiElement> sourceElement) {
        PsiClass benchmarkClass = getBenchmarkClass(context);
        if (benchmarkClass == null) {
            return false;
        }
        sourceElement.set(benchmarkClass);
        setupConfigurationModule(context, configuration);
        final Module originalModule = configuration.getConfigurationModule().getModule();
        configuration.restoreOriginalModule(originalModule);
        configuration.setProgramParameters(benchmarkClass.getQualifiedName() + ".*");
        configuration.setName(benchmarkClass.getName());

        configuration.setType(JmhConfiguration.Type.CLASS);
        return true;
    }

    @Override
    public boolean isConfigurationFromContext(JmhConfiguration configuration, ConfigurationContext context) {
        if (configuration.getBenchmarkType() != JmhConfiguration.Type.CLASS) {
            return false;
        }
        PsiClass benchmarkClass = getBenchmarkClass(context);
        if (benchmarkClass == null) {
            return false;
        }
        String nameFromContext = benchmarkClass.getName();
        if (configuration.getName() == null || !configuration.getName().equals(nameFromContext)) {
            return false;
        }
        Location location = JavaExecutionUtil.stepIntoSingleClass(context.getLocation());
        final Module originalModule = configuration.getConfigurationModule().getModule();
        if (location.getModule() == null || !location.getModule().equals(originalModule)) {
            return false;
        }
        setupConfigurationModule(context, configuration);
        configuration.restoreOriginalModule(originalModule);

        return true;
    }

    private PsiClass getBenchmarkClass(ConfigurationContext context) {
        Location<?> location = context.getLocation();
        if (location == null) {
            return null;
        }
        for (Iterator<Location<PsiClass>> iterator = location.getAncestors(PsiClass.class, false); iterator.hasNext();) {
            final Location<PsiClass> classLocation = iterator.next();
            if (hasBenchmarks(classLocation.getPsiElement())) {
                return classLocation.getPsiElement();
            }
        }
        return null;
    }

    private boolean hasBenchmarks(PsiClass psiClass) {
        for (PsiMethod method : psiClass.getMethods()) {
            if (ConfigurationUtils.hasBenchmarkAnnotation(method)) {
                return true;
            }
        }
        return false;
    }
}
