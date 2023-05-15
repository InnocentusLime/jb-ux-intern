package com.github.innocentuslime.jbuxintern.toolWindow

import com.github.innocentuslime.jbuxintern.MyBundle
import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.psi.*
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.ui.components.JBPanel
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.containers.stream
import java.util.stream.Collectors
import javax.swing.tree.DefaultMutableTreeNode


class ClassCountWindowFactory : ToolWindowFactory {
    private val contentFactory = ContentFactory.SERVICE.getInstance()

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val myToolWindow = MyToolWindow(classesAndMethodsInProject(project))
        val content = contentFactory.createContent(myToolWindow.getContent(), null, false)
        toolWindow.contentManager.addContent(content)
    }

    override fun shouldBeAvailable(project: Project) = true

    private class MyToolWindow(private val view: Map<String, Pair<Int, Int>>) {

        fun getContent() = JBPanel<JBPanel<*>>().apply {
            val root = DefaultMutableTreeNode(MyBundle.message("classAndMethodCountHeading"))
            val tree = Tree(root)

            view.forEach { (name, counts) ->
                val (classCount, methodCount) = counts
                val fileNode = DefaultMutableTreeNode(name)

                fileNode.add(DefaultMutableTreeNode(MyBundle.message("classCount", classCount)))
                fileNode.add(DefaultMutableTreeNode(MyBundle.message("methodCount", methodCount)))
                root.add(fileNode)
            }

            add(tree)
        }
    }
}


private fun countClassesAndFunctions(cl: PsiClass): Pair<Int, Int> {
    var classCounter = 0
    var funcCounter = 0
    cl.accept(object : JavaRecursiveElementVisitor() {
        override fun visitClass(aClass: PsiClass?) {
            classCounter += 1
            if (aClass == null) {
                return
            }

            aClass.innerClasses.forEach { x -> x.accept(this) }
            aClass.methods.forEach { x -> x.accept(this) }
        }

        override fun visitMethod(method: PsiMethod?) {
            funcCounter += 1
            if (method == null) {
                return
            }

            method.body?.accept(this)
        }
    })

    return Pair(classCounter, funcCounter)
}

private fun classesAndMethodsInFile(file: PsiJavaFile): Pair<Int, Int> {
    return file.classes.stream().map { x -> countClassesAndFunctions(x) }
        .reduce(Pair(0, 0)) {
                (x1, y1), (x2, y2) -> Pair(x1 + x2, y1 + y2)
        }
}

fun classesAndMethodsInProject(project: Project): Map<String, Pair<Int, Int>> {
    val psiManager = PsiManager.getInstance(project)
    val projectScope = GlobalSearchScope.projectScope(project)

    return FileTypeIndex.getFiles(
        JavaFileType.INSTANCE,
        projectScope,
    ).stream()
        .map(psiManager::findFile)
        .filter { it is PsiJavaFile }
        .map { it as PsiJavaFile }
        .collect(Collectors.toMap(
            { it.name },
            { classesAndMethodsInFile(it) },
        ))
}
