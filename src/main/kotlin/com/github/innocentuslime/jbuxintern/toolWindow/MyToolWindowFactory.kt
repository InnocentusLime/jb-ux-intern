package com.github.innocentuslime.jbuxintern.toolWindow

import com.github.innocentuslime.jbuxintern.MyBundle
import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.psi.JavaRecursiveElementVisitor
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.content.ContentFactory
import javax.swing.JButton


class MyToolWindowFactory : ToolWindowFactory {

    init {
        thisLogger().warn("Don't forget to remove all non-needed sample code files with their corresponding registration entries in `plugin.xml`.")
    }

    private val contentFactory = ContentFactory.SERVICE.getInstance()

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val myToolWindow = MyToolWindow(toolWindow)
        val content = contentFactory.createContent(myToolWindow.getContent(), null, false)
        toolWindow.contentManager.addContent(content)
    }

    override fun shouldBeAvailable(project: Project) = true

    class MyToolWindow(toolWindow: ToolWindow) {

        private val psiManager = PsiManager.getInstance(toolWindow.project)
        private val projectScope = GlobalSearchScope.projectScope(toolWindow.project)

        private fun countClasses(cl: PsiClass): Int {
            var counter = 0
            cl.accept(object : JavaRecursiveElementVisitor() {
                override fun visitClass(aClass: PsiClass?) {
                    counter += 1
                    if (aClass == null) {
                        return
                    }

                    aClass.allInnerClasses.forEach { x -> x.accept(this) }
                    aClass.allMethods.forEach { x -> x.accept(this) }
                }
            })

            return counter
        }

        private fun classesInFile(cls: Array<PsiClass>): Int {
            return cls.sumOf { x -> countClasses(x) }
        }

        fun getContent() = JBPanel<JBPanel<*>>().apply {
            val label = JBLabel(MyBundle.message("classCount", "?"))

            add(label)
            add(JButton(MyBundle.message("countClasses")).apply {
                addActionListener {
                    FileTypeIndex.getFiles(
                        JavaFileType.INSTANCE,
                        projectScope,
                    ).stream()
                        .map(psiManager::findFile)
                        .filter { it is PsiJavaFile }
                        .map { it as PsiJavaFile }
                        .forEach { file -> thisLogger().warn("file ${file.name} has ${classesInFile(file.classes)} classes") }

                    //label.text = MyBundle.message("classCount", counter)
                }
            })
        }
    }
}
