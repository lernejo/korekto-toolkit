# Exercise started
Your current grade is **[(${{grade}})]**/[(${{maxGrade}})].  

## Detail[# th:each="part : ${gradeParts}"]
* [(${part.id})]: [(${{part.grade}})][# th:if="${part.maxGrade}"]/[(${{part.maxGrade}})][/][# th:each="comment : ${part.comments}"]
    * [(${comment})]
[/][/]


[# th:if="${deadline}"]You have until [(${deadline})] to improve your grade.

[/]*Analyze done at [(${now})].*
