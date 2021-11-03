import { Interpreter, NoteType } from './note-options';
export interface NoteExtra {
  interpreter: Interpreter;
  noteType: NoteType;
  platform: string;
}
export interface NoteInfo {
  name: string;
  folder: string;
  nameError?: string;
}
export interface NoteForm extends NoteInfo, NoteExtra {

}
