# project.kitchen
Android Application for discovering, organizing, and creating recipes

Personal project created to make my own life easier, but welcome to others if they're interested in cooking!

[BETA Testing on Google Play Store here](https://play.google.com/store/apps/details?id=project.kitchen) 

This application offers a great way to discover new recipes by pulling recipes from multiple different sources and 
collating them to a single, unified data model. Allows users to edit or create their own recipes, favorite them for easy access, 
and even create recipe books to organize them to a single large meal like "Christmas Dinner" or just meals they like to make in 
general like "5-min Dinners."

 - Utilizes GcmTaskSchedule to schedule daily imports of new recipes
 - RecyclerView to recycle Views
 - SQliteDatabase and ContentProvider to store and access data
 - Unique tablet UI featuring master-detali-flow
 - Transition and object animations
 - ItemTouchHelpers for manipulation of items in RecyclerViews
