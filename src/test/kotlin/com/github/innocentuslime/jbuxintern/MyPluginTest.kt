package com.github.innocentuslime.jbuxintern

import com.intellij.testFramework.TestDataPath
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.github.innocentuslime.jbuxintern.toolWindow.classesAndMethodsInProject

@TestDataPath("\$CONTENT_ROOT/src/test/testData")
class MyPluginTest : BasePlatformTestCase() {

    fun testCounting() {
        myFixture.copyFileToProject("myClass.java")
        myFixture.copyFileToProject("myNewClass.java")
        val map = classesAndMethodsInProject(project)

        assert(map["myClass.java"]?.let { it.first == 10 && it.second == 2 }!!)
        assert(map["myNewClass.java"]?.let { it.first == 1 && it.second == 0 }!!)
    }

    override fun getTestDataPath() = "src/test/testData/counting"
}
