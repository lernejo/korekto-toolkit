# Exercise started
Your current grade is **[(${{grade}})]**/[(${{maxGrade}})].  

## Detail[# th:each="part : ${gradeParts}"]
* [(${part.id})]: [(${{part.grade}})]/[(${{part.maxGrade}})][# th:each="comment : ${part.comments}"]
    * [(${comment})]
[/][/]


You have until [(${deadline})] to improve your grade.

*Analyzed done at [(${now})].*
