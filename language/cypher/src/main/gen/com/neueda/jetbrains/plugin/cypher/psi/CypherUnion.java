// This is a generated file. Not intended for manual editing.
package com.neueda.jetbrains.plugin.cypher.psi;

import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;

public interface CypherUnion extends PsiElement {

  @NotNull
  CypherSingleQuery getSingleQuery();

  @Nullable
  PsiElement getKAll();

  @NotNull
  PsiElement getKUnion();

}