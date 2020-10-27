package com.zs.annotation_compiler;

import com.google.auto.service.AutoService;
import com.zs.annotation.BindString;
import com.zs.annotation.BindView;
import com.zs.annotation.OnClick;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Name;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

/**
 * 目的：生成一个java文件 去完成findViewById onClick 获取String资源的代码
 * <p>
 * 注解处理器
 * 1、集成
 * 2、注册服务
 */
@AutoService(Processor.class)
public class AnnotationCompiler extends AbstractProcessor {
    /**
     * 生成文件的对象
     */
    Filer filer;

    @Override
    public synchronized void init(ProcessingEnvironment env) {
        super.init(env);
        filer = env.getFiler();
    }

    /**
     * 声明要处理的注解有哪些
     *
     * @return
     */
    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> types = new HashSet<>();
        types.add(BindView.class.getCanonicalName());
        types.add(OnClick.class.getCanonicalName());
        types.add(BindString.class.getCanonicalName());
        return types;
    }

    /**
     * 声明支持java的版本
     *
     * @return
     */
    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latest();
    }

    /**
     * 核心方法 这个方法可以得到注解所标记的内容
     *
     * @param set
     * @param roundEnvironment
     * @return
     */
    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        System.out.println("process in ------------->");
        //获取BindView 注解所修饰的属性
        Set<? extends Element> elements = roundEnvironment.getElementsAnnotatedWith(BindView.class);
        //节点
        //类节点 TypeElement
        //方法节点 ExecutableElement
        //成员变量 VariableElement
        //把每个Activity类中被标记的节点区分开来 然后统一的存储起来
        Map<String, List<VariableElement>> map = new HashMap<>();
        for (Element element : elements) {
            VariableElement variableElement = (VariableElement) element;
            //获取上一个节点 获取类名
            TypeElement typeElement = (TypeElement) variableElement.getEnclosingElement();
            String activityName = typeElement.getSimpleName().toString();
            List<VariableElement> variableElements = map.get(activityName);
            if (variableElements == null) {
                variableElements = new ArrayList<>();
                map.put(activityName, variableElements);
            }
            variableElements.add(variableElement);
        }
        if (map.size() > 0) {
            Writer writer = null;
            Iterator<String> iterator = map.keySet().iterator();
            while (iterator.hasNext()) {
                String activityName = iterator.next();
                List<VariableElement> variableElements = map.get(activityName);
                String packageName = getPackageName(variableElements.get(0));
                String newActivityName = activityName + "$$ViewBinder";
                try {
                    JavaFileObject sourceFile = filer.createSourceFile(packageName + "." + newActivityName);
                    writer = sourceFile.openWriter();
                    StringBuffer stringBuffer = new StringBuffer();
                    stringBuffer.append("package " + packageName + ";\n");
                    stringBuffer.append("import android.view.View;\n");
                    stringBuffer.append("public class " + newActivityName + "{\n");
                    stringBuffer.append("public " + newActivityName + "(final " + packageName + "." + activityName + " target) {\n");
                    for (VariableElement variableElement : variableElements) {
                        int resId = variableElement.getAnnotation(BindView.class).value();
                        String fileName = variableElement.getSimpleName().toString();
                        stringBuffer.append("target." + fileName +" = " + "target.findViewById(" + resId + ");\n");
                    }

                    stringBuffer.append("}\n}");
                    writer.write(stringBuffer.toString());
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (writer != null) {
                        try {
                            writer.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * 获取包名的方法
     *
     * @param variableElement
     * @return
     */
    public String getPackageName(VariableElement variableElement) {
        TypeElement typeElement = (TypeElement) variableElement.getEnclosingElement();
        //获取包名
        PackageElement packageElement = processingEnv.getElementUtils().getPackageOf(typeElement);
        Name qualifiedName = packageElement.getQualifiedName();
        return qualifiedName.toString();
    }
}
